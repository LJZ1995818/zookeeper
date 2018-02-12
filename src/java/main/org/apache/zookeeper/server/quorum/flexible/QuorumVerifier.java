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

package org.apache.zookeeper.server.quorum.flexible;

import java.util.Set;
import java.util.Map;

import org.apache.zookeeper.server.quorum.QuorumPeer.QuorumServer;

/**
 * All quorum validators have to implement a method called
 * containsQuorum, which verifies if a HashSet of server 
 * identifiers constitutes a quorum.
 * 所有仲裁验证程序都必须实现一个名为
 * containsQuorum, 它验证服务器是否 HashSet
 * 标识符构成法定人数。
 */

public interface QuorumVerifier {
    long getWeight(long id);

    boolean containsQuorum(Set<Long> set);

    long getVersion();

    void setVersion(long ver);

    Map<Long, QuorumServer> getAllMembers();

    /**
     * 选举成员
     * @return
     */
    Map<Long, QuorumServer> getVotingMembers();

    /**
     * 观察者成员
     * @return
     */
    Map<Long, QuorumServer> getObservingMembers();

    boolean equals(Object o);

    String toString();
}
