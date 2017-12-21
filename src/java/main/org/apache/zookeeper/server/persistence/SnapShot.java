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
import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.zookeeper.server.DataTree;

/**
 * snapshot interface for the persistence layer.
 * implement this interface for implementing 
 * snapshots.
 * 持久层的快照接口，实现接口用于实现快照功能
 */
public interface SnapShot {
    
    /**
     * deserialize a data tree from the last valid snapshot and 
     * return the last zxid that was deserialized
     * 从最近有效的快照中饭序列化一个数据树，并返回最新的zxid
     * @param dt the datatree to be deserialized into
     * @param sessions the sessions to be deserialized into
     * @return the last zxid that was deserialized from the snapshot
     * @throws IOException
     */
    long deserialize(DataTree dt, Map<Long, Integer> sessions) 
        throws IOException;
    
    /**
     * persist the datatree and the sessions into a persistence storage
     * 持久化数据树和会话到一个文件中
     * @param dt the datatree to be serialized
     * @param sessions the session timeouts to be serialized
     * @param name the object name to store snapshot into
     * @param fsync sync the snapshot immediately after write
     * @throws IOException
     */
    void serialize(DataTree dt, Map<Long, Integer> sessions,
                   File name, boolean fsync)
        throws IOException;
    
    /**
     * find the most recent snapshot file
     * 发现最近的快照记录
     * @return the most recent snapshot file
     * @throws IOException
     */
    File findMostRecentSnapshot() throws IOException;
    
    /**
     * free resources from this snapshot immediately
     * 立即从该快照获得释放资源
     * @throws IOException
     */
    void close() throws IOException;
} 
