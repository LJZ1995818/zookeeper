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

package org.apache.zookeeper.server;

import java.util.Date;

/**
 * Statistics on the ServerCnxn
 * ServerCnxn的 状态接口
 */
interface Stats {
    /** 
     * 获取建立连接的时间
     * @since 3.3.0 */
    Date getEstablished();

    /**
     * 已提交但尚未答复的请求数。
     */
    long getOutstandingRequests();
    /** 收到的package数目*/
    long getPacketsReceived();
    /** Number of packets sent (incl notifications) */
    long getPacketsSent();
    /** 最小延迟 ms
     * @since 3.3.0 */
    long getMinLatency();
    /** 平均延迟 ms
     * @since 3.3.0 */
    long getAvgLatency();
    /** 最大延迟
     * @since 3.3.0 */
    long getMaxLatency();
    /** 此连接的上一个操作
     * @since 3.3.0 */
    String getLastOperation();
    /** Last cxid of this connection
     * @since 3.3.0 */
    long getLastCxid();
    /** Last zxid of this connection
     * @since 3.3.0 */
    long getLastZxid();
    /** 上次服务器在该连接上向客户端发送响应
     * @since 3.3.0 */
    long getLastResponseTime();
    /** 此连接上的客户端上一次响应的滞后时间 ms
     * @since 3.3.0 */
    long getLastLatency();

    /** 重置状态
     * @since 3.3.0 */
    void resetStats();
}
