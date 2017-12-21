/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zookeeper.server.persistence;

import java.io.IOException;

import org.apache.jute.Record;
import org.apache.zookeeper.txn.TxnHeader;

/**
 * Interface for reading transaction logs.
 * 读取事务性日志的接口
 *
 */
public interface TxnLog {
    
    /**
     * roll the current
     * log being appended to
     * 回滚当前日志
     * @throws IOException 
     */
    void rollLog() throws IOException;
    /**
     * Append a request to the transaction log
     * 添加一个请求到事务日志中
     * @param hdr the transaction header
     * @param r the transaction itself
     * returns true iff something appended, otw false 
     * @throws IOException
     */
    boolean append(TxnHeader hdr, Record r) throws IOException;

    /**
     * Start reading the transaction logs
     * from a given zxid
     * 根据一个给定zxid，读取事务日志
     * @param zxid
     * @return returns an iterator to read the 
     * next transaction in the logs.
     * @throws IOException
     */
    TxnIterator read(long zxid) throws IOException;
    
    /**
     * the last zxid of the logged transactions.
     * 获取最新的事务zxid
     * @return the last zxid of the logged transactions.
     * @throws IOException
     */
    long getLastLoggedZxid() throws IOException;
    
    /**
     * truncate the log to get in sync with the leader.
     * 截断日志以使其与learder同步
     * @param zxid the zxid to truncate at.
     * @throws IOException 
     */
    boolean truncate(long zxid) throws IOException;
    
    /**
     * the dbid for this transaction log. 
     * 获取事务性日志的数据库ID
     * @return the dbid for this transaction log.
     * @throws IOException
     */
    long getDbId() throws IOException;
    
    /**
     * commit the transaction and make sure
     * they are persisted
     * 提交事务并确认他们已经持久化
     * @throws IOException
     */
    void commit() throws IOException;

    /**
     *
     * @return transaction log's elapsed sync time in milliseconds
     */
    long getTxnLogSyncElapsedTime();
   
    /** 
     * close the transactions logs
     * 关闭事务日志
     */
    void close() throws IOException;
    /**
     * an iterating interface for reading 
     * transaction logs.
     * 一个迭代接口用于读取事务日志
     */
    public interface TxnIterator {
        /**
         * return the transaction header.
         * @return return the transaction header.
         */
        TxnHeader getHeader();
        
        /**
         * return the transaction record.
         * @return return the transaction record.
         */
        Record getTxn();
     
        /**
         * go to the next transaction record.
         * @throws IOException
         */
        boolean next() throws IOException;
        
        /**
         * close files and release the 
         * resources
         * @throws IOException
         */
        void close() throws IOException;
        
        /**
         * Get an estimated storage space used to store transaction records
         * that will return by this iterator
         * @throws IOException
         */
        long getStorageSize() throws IOException;
    }
}

