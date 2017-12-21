/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zookeeper.server.persistence;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.jute.Record;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.ZooDefs.OpCode;
import org.apache.zookeeper.server.DataTree;
import org.apache.zookeeper.server.DataTree.ProcessTxnResult;
import org.apache.zookeeper.server.Request;
import org.apache.zookeeper.server.ZooTrace;
import org.apache.zookeeper.server.persistence.TxnLog.TxnIterator;
import org.apache.zookeeper.txn.CreateSessionTxn;
import org.apache.zookeeper.txn.TxnHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a helper class
 * above the implementations
 * of txnlog and snapshot
 * classes
 */
public class FileTxnSnapLog {
    /**
     * 这个目录包含事务日志
     */
    private final File dataDir;
    /**
     * 这个目录包含快照日志
     */
    private final File snapDir;
    private TxnLog txnLog;
    private SnapShot snapLog;
    private final boolean autoCreateDB;
    public final static int VERSION = 2;
    public final static String version = "version-";

    private static final Logger LOG = LoggerFactory.getLogger(FileTxnSnapLog.class);

    public static final String ZOOKEEPER_DATADIR_AUTOCREATE =
            "zookeeper.datadir.autocreate";

    public static final String ZOOKEEPER_DATADIR_AUTOCREATE_DEFAULT = "true";

    static final String ZOOKEEPER_DB_AUTOCREATE = "zookeeper.db.autocreate";

    private static final String ZOOKEEPER_DB_AUTOCREATE_DEFAULT = "true";

    /**
     * This listener helps
     * the external apis calling
     * restore to gather information
     * while the data is being
     * restored.
     * 这个监听器帮助于在当数据被恢复的时候，
     * 外部api调用restore()时，用于收集数据
     */
    public interface PlayBackListener {
        void onTxnLoaded(TxnHeader hdr, Record rec);
    }

    /**
     * the constructor which takes the datadir and snapdir.
     *
     * @param dataDir the transaction directory
     * @param snapDir the snapshot directory
     */
    public FileTxnSnapLog(File dataDir, File snapDir) throws IOException {
        LOG.debug("Opening datadir:{} snapDir:{}", dataDir, snapDir);

        this.dataDir = new File(dataDir, version + VERSION);
        this.snapDir = new File(snapDir, version + VERSION);

        // by default create snap/log dirs, but otherwise complain instead
        // See ZOOKEEPER-1161 for more details
        // 默认情况下创建快照/日志目录, 
        boolean enableAutocreate = Boolean.valueOf(
                System.getProperty(ZOOKEEPER_DATADIR_AUTOCREATE,
                        ZOOKEEPER_DATADIR_AUTOCREATE_DEFAULT));

        if (!this.dataDir.exists()) {
            if (!enableAutocreate) {
                throw new DatadirException("Missing data directory "
                        + this.dataDir
                        + ", automatic data directory creation is disabled ("
                        + ZOOKEEPER_DATADIR_AUTOCREATE
                        + " is false). Please create this directory manually.");
            }

            if (!this.dataDir.mkdirs()) {
                throw new DatadirException("Unable to create data directory "
                        + this.dataDir);
            }
        }
        if (!this.dataDir.canWrite()) {
            throw new DatadirException("Cannot write to data directory " + this.dataDir);
        }

        if (!this.snapDir.exists()) {
            // by default create this directory, but otherwise complain instead
            // See ZOOKEEPER-1161 for more details
            if (!enableAutocreate) {
                throw new DatadirException("Missing snap directory "
                        + this.snapDir
                        + ", automatic data directory creation is disabled ("
                        + ZOOKEEPER_DATADIR_AUTOCREATE
                        + " is false). Please create this directory manually.");
            }

            if (!this.snapDir.mkdirs()) {
                throw new DatadirException("Unable to create snap directory "
                        + this.snapDir);
            }
        }
        if (!this.snapDir.canWrite()) {
            throw new DatadirException("Cannot write to snap directory " + this.snapDir);
        }

        txnLog = new FileTxnLog(this.dataDir);
        snapLog = new FileSnap(this.snapDir);

        autoCreateDB = Boolean.parseBoolean(System.getProperty(ZOOKEEPER_DB_AUTOCREATE,
                ZOOKEEPER_DB_AUTOCREATE_DEFAULT));
    }

    /**
     * get the datadir used by this filetxn
     * snap log
     *
     * @return the data dir
     */
    public File getDataDir() {
        return this.dataDir;
    }

    /**
     * get the snap dir used by this
     * filetxn snap log
     *
     * @return the snap dir
     */
    public File getSnapDir() {
        return this.snapDir;
    }

    /**
     * 这个功能可实现在读取快照和事务日志之后，恢复服务器数据库
     *
     * @param dt       the datatree to be restored
     * @param sessions the sessions to be restored
     * @param listener the playback listener to run on the
     *                 database restoration
     * @return 恢复的最高的zxid
     * @throws IOException
     */
    public long restore(DataTree dt, Map<Long, Integer> sessions,
                        PlayBackListener listener) throws IOException {
        long deserializeResult = snapLog.deserialize(dt, sessions);// 从快照中获取最新的zxid
        FileTxnLog txnLog = new FileTxnLog(dataDir);
        boolean trustEmptyDB;
        File initFile = new File(dataDir.getParent(), "initialize");
        if (Files.deleteIfExists(initFile.toPath())) {
            LOG.info("Initialize file found, an empty database will not block voting participation");
            trustEmptyDB = true;
        } else {
            trustEmptyDB = autoCreateDB;
        }
        if (-1L == deserializeResult) {
            /* this means that we couldn't find any snapshot, so we need to
             * initialize an empty database (reported in ZOOKEEPER-2325) */
            if (txnLog.getLastLoggedZxid() != -1) {
                throw new IOException(
                        "No snapshot found, but there are log entries. " +
                                "Something is broken!");
            }

            if (trustEmptyDB) {
                /* TODO: (br33d) we should either put a ConcurrentHashMap on restore()
                 *       or use Map on save() */
                save(dt, (ConcurrentHashMap<Long, Integer>) sessions, false);

                /* 返回一个为0的 zxid，因为我们知道数据库是空的 */
                return 0L;
            } else {
                /* 返回一个-1的zxid, 因为我们可能缺少数据 */
                LOG.warn("Unexpected empty data tree, setting zxid to -1");
                dt.lastProcessedZxid = -1L;
                return -1L;
            }
        }
        TxnIterator itr = txnLog.read(dt.lastProcessedZxid + 1);
        long highestZxid = dt.lastProcessedZxid;// 从已知情况中获取的最高的zxid
        TxnHeader hdr;
        try {
            while (true) {
                // iterator points to
                // the first valid txn when initialized
                // 当恢复数据时应该从最近的一个有效的txn 中恢复
                hdr = itr.getHeader();
                if (hdr == null) {
                    //empty logs
                    return dt.lastProcessedZxid;
                }
                if (hdr.getZxid() < highestZxid && highestZxid != 0) {
                    LOG.error("{}(highestZxid) > {}(next log) for type {}",
                            highestZxid, hdr.getZxid(), hdr.getType());
                } else {
                    highestZxid = hdr.getZxid();
                }
                try {
                    processTransaction(hdr, dt, sessions, itr.getTxn());
                } catch (KeeperException.NoNodeException e) {
                    throw new IOException("Failed to process transaction type: " +
                            hdr.getType() + " error: " + e.getMessage(), e);
                }
                listener.onTxnLoaded(hdr, itr.getTxn());// 执行用于自定义的收集事务信息
                if (!itr.next())
                    break;
            }
        } finally {
            if (itr != null) {
                itr.close();
            }
        }
        return highestZxid;// 返回最新的zxid
    }

    /**
     * Get TxnIterator for iterating through txnlog starting at a given zxid
     *
     * @param zxid starting zxid
     * @return TxnIterator
     * @throws IOException
     */
    public TxnIterator readTxnLog(long zxid) throws IOException {
        return readTxnLog(zxid, true);
    }

    /**
     * Get TxnIterator for iterating through txnlog starting at a given zxid
     *
     * @param zxid        starting zxid
     * @param fastForward true if the iterator should be fast forwarded to point
     *                    to the txn of a given zxid, else the iterator will point to the
     *                    starting txn of a txnlog that may contain txn of a given zxid
     * @return TxnIterator
     * @throws IOException
     */
    public TxnIterator readTxnLog(long zxid, boolean fastForward)
            throws IOException {
        FileTxnLog txnLog = new FileTxnLog(dataDir);
        return txnLog.read(zxid, fastForward);
    }

    /**
     * process the transaction on the datatree
     * 在数据数据树中运行事务
     *
     * @param hdr      the hdr of the transaction
     * @param dt       the datatree to apply transaction to
     * @param sessions the sessions to be restored
     * @param txn      被应用的事务
     */
    public void processTransaction(TxnHeader hdr, DataTree dt,
                                   Map<Long, Integer> sessions, Record txn)
            throws KeeperException.NoNodeException {
        ProcessTxnResult rc;
        switch (hdr.getType()) {
            case OpCode.createSession:// 创建session，并将session插入到sessions中
                sessions.put(hdr.getClientId(),
                        ((CreateSessionTxn) txn).getTimeOut());
                if (LOG.isTraceEnabled()) {
                    ZooTrace.logTraceMessage(LOG, ZooTrace.SESSION_TRACE_MASK,
                            "playLog --- create session in log: 0x"
                                    + Long.toHexString(hdr.getClientId())
                                    + " with timeout: "
                                    + ((CreateSessionTxn) txn).getTimeOut());
                }
                // give dataTree a chance to sync its lastProcessedZxid
                rc = dt.processTxn(hdr, txn);
                break;
            case OpCode.closeSession:// 关闭session，并移除session中的指定信息
                sessions.remove(hdr.getClientId());
                if (LOG.isTraceEnabled()) {
                    ZooTrace.logTraceMessage(LOG, ZooTrace.SESSION_TRACE_MASK,
                            "playLog --- close session in log: 0x"
                                    + Long.toHexString(hdr.getClientId()));
                }
                rc = dt.processTxn(hdr, txn);
                break;
            default:
                rc = dt.processTxn(hdr, txn);// 对datatree 执行事务日志内容
        }

        /**
         * Snapshots are lazily created. So when a snapshot is in progress,
         * there is a chance for later transactions to make into the
         * snapshot. Then when the snapshot is restored, NONODE/NODEEXISTS
         * errors could occur. It should be safe to ignore these.
         * 快照是惰性创建的。因此，当快照正在进行时，有可能之后的事务已执行到快照中，
         * 然后当恢复快照时，NONODE/NODEEXISTS 的错误是有可能发生的，在这是应该被安全忽略的
         */
        if (rc.err != Code.OK.intValue()) {
            LOG.debug(
                    "Ignoring processTxn failure hdr: {}, error: {}, path: {}",
                    hdr.getType(), rc.err, rc.path);
        }
    }

    /**
     * the last logged zxid on the transaction logs
     *
     * @return the last logged zxid
     */
    public long getLastLoggedZxid() {
        FileTxnLog txnLog = new FileTxnLog(dataDir);
        return txnLog.getLastLoggedZxid();
    }

    /**
     * save the datatree and the sessions into a snapshot
     * 存储datatree 和 session 到一个快照中，用于初始化快照
     *
     * @param dataTree             the datatree to be serialized onto disk
     * @param sessionsWithTimeouts the session timeouts to be
     *                             serialized onto disk
     * @param syncSnap             sync the snapshot immediately after write
     * @throws IOException
     */
    public void save(DataTree dataTree,
                     ConcurrentHashMap<Long, Integer> sessionsWithTimeouts,
                     boolean syncSnap)
            throws IOException {
        long lastZxid = dataTree.lastProcessedZxid;
        File snapshotFile = new File(snapDir, Util.makeSnapshotName(lastZxid));
        LOG.info("Snapshotting: 0x{} to {}", Long.toHexString(lastZxid),
                snapshotFile);
        snapLog.serialize(dataTree, sessionsWithTimeouts, snapshotFile, syncSnap);

    }

    /**
     * truncate the transaction logs the zxid specified
     *
     * @param zxid the zxid to truncate the logs to
     * @return true if able to truncate the log, false if not
     * @throws IOException
     */
    public boolean truncateLog(long zxid) throws IOException {
        // close the existing txnLog and snapLog
        close();

        // truncate it
        FileTxnLog truncLog = new FileTxnLog(dataDir);
        boolean truncated = truncLog.truncate(zxid);
        truncLog.close();

        // re-open the txnLog and snapLog
        // I'd rather just close/reopen this object itself, however that 
        // would have a big impact outside ZKDatabase as there are other
        // objects holding a reference to this object.
//        重新打开 txnLog 和 snapLog
//        我宁愿只是关闭/重新打开这个对象本身, 但
//        将有一个大的影响外 ZKDatabase, 因为有其他
//        保存对此对象的引用的对象。
        txnLog = new FileTxnLog(dataDir);
        snapLog = new FileSnap(snapDir);

        return truncated;
    }

    /**
     * the most recent snapshot in the snapshot
     * directory
     *
     * @return the file that contains the most
     * recent snapshot
     * @throws IOException
     */
    public File findMostRecentSnapshot() throws IOException {
        FileSnap snaplog = new FileSnap(snapDir);
        return snaplog.findMostRecentSnapshot();
    }

    /**
     * the n most recent snapshots
     *
     * @param n the number of recent snapshots
     * @return the list of n most recent snapshots, with
     * the most recent in front
     * @throws IOException
     */
    public List<File> findNRecentSnapshots(int n) throws IOException {
        FileSnap snaplog = new FileSnap(snapDir);
        return snaplog.findNRecentSnapshots(n);
    }

    /**
     * get the snapshot logs which may contain transactions newer than the given zxid.
     * This includes logs with starting zxid greater than given zxid, as well as the
     * newest transaction log with starting zxid less than given zxid.  The latter log
     * file may contain transactions beyond given zxid.
     *
     * @param zxid the zxid that contains logs greater than
     *             zxid
     * @return
     */
    public File[] getSnapshotLogs(long zxid) {
        return FileTxnLog.getLogFiles(dataDir.listFiles(), zxid);
    }

    /**
     * append the request to the transaction logs
     *
     * @param si the request to be appended
     *           returns true iff something appended, otw false
     * @throws IOException
     */
    public boolean append(Request si) throws IOException {
        return txnLog.append(si.getHdr(), si.getTxn());
    }

    /**
     * 提交事务日志
     *
     * @throws IOException
     */
    public void commit() throws IOException {
        txnLog.commit();
    }

    /**
     * @return elapsed sync time of transaction log commit in milliseconds
     */
    public long getTxnLogElapsedSyncTime() {
        return txnLog.getTxnLogSyncElapsedTime();
    }

    /**
     * roll the transaction logs
     *
     * @throws IOException
     */
    public void rollLog() throws IOException {
        txnLog.rollLog();
    }

    /**
     * 关闭当前的快照文件和事务文件
     *
     * @throws IOException
     */
    public void close() throws IOException {
        txnLog.close();
        snapLog.close();
    }

    /**
     * 创建目录失败的异常
     */
    @SuppressWarnings("serial")
    public static class DatadirException extends IOException {
        public DatadirException(String msg) {
            super(msg);
        }

        public DatadirException(String msg, Exception e) {
            super(msg, e);
        }
    }
}
