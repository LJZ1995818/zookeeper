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

package org.apache.zookeeper.client;

import org.apache.yetus.audience.InterfaceAudience;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;

/**
 * A set of hosts a ZooKeeper client should connect to.
 * 管理员应连接的一组主机。
 * 
 * Classes implementing this interface must guarantee the following:
 * 实现此接口的类必须保证以下内容:
 * 
 * * Every call to next() returns an InetSocketAddress. So the iterator never
 * ends.
 * 每个调用 next() 返回一个 InetSocketAddress。所以迭代器从来没有结束.
 * * The size() of a HostProvider may never be zero.
 * HostProvider 的 size() 可能永远不会为零。
 * A HostProvider must return resolved InetSocketAddress instances on next(),
 * but it's up to the HostProvider, when it wants to do the resolving.
 * 
 * Different HostProvider could be imagined:
 * 
 * * A HostProvider that loads the list of Hosts from an URL or from DNS 
 * * A HostProvider that re-resolves the InetSocketAddress after a timeout. 
 * * A HostProvider that prefers nearby hosts.
 */
@InterfaceAudience.Public
public interface HostProvider {
    public int size();

    /**
     * The next host to try to connect to.
     * 
     * For a spinDelay of 0 there should be no wait.
     * 
     * @param spinDelay
     *            Milliseconds to wait if all hosts have been tried once.
     */
    public InetSocketAddress next(long spinDelay);

    /**
     * Notify the HostProvider of a successful connection.
     * 
     * The HostProvider may use this notification to reset it's inner state.
     */
    public void onConnected();

    /**
     * Update the list of servers. This returns true if changing connections is necessary for load-balancing, false otherwise.
     * @param serverAddresses new host list
     * @param currentHost the host to which this client is currently connected
     * @return true if changing connections is necessary for load-balancing, false otherwise  
     */
    boolean updateServerList(Collection<InetSocketAddress> serverAddresses,
        InetSocketAddress currentHost);
}
