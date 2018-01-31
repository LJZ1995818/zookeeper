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

package org.apache.zookeeper.server.auth;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.server.ServerCnxn;

/**
 * This interface is implemented by authentication providers to add new kinds of
 * authentication schemes to ZooKeeper.
 * 此接口由认证供应商实现了添加认证方案。
 */
public interface AuthenticationProvider {
    /**
     * The String used to represent this provider. This will correspond to the
     * scheme field of an Id.
     * 
     * @return the scheme of this provider.
     */
    String getScheme();

    /**
     * This method is called when a client passes authentication data for this
     * scheme. The authData is directly from the authentication packet. The
     * implementor may attach new ids to the authInfo field of cnxn or may use
     * cnxn to send packets back to the client.
     * 
     * @param cnxn
     *                the cnxn that received the authentication information.
     * @param authData
     *                the authentication data received.
     * @return TODO
     */
    KeeperException.Code handleAuthentication(ServerCnxn cnxn, byte authData[]);

    /**
     * This method is called to see if the given id matches the given id
     * expression in the ACL. This allows schemes to use application specific
     * wild cards.
     * 此方法被调用，以确定给定ID是否与ACL中给定的id表达式相匹配。
     * 这使得方案可以使用特定于应用程序的通配符。
     * 
     * @param id
     *                the id to check.
     * @param aclExpr
     *                the expression to match ids against.
     * @return true if the id can be matched by the expression.
     */
    boolean matches(String id, String aclExpr);

    /**
     * This method is used to check if the authentication done by this provider
     * should be used to identify the creator of a node. Some ids such as hosts
     * and ip addresses are rather transient and in general don't really
     * identify a client even though sometimes they do.
     * 此方法用于检查该提供者所做的身份验证是否应用于标识节点的创建者。
     * 有些ID，如主机和IP地址是相当短暂的，一般来说，并没有真正识别客户机，尽管有时他们会这样做。
     * 
     * @return true if this provider identifies creators.
     */
    boolean isAuthenticated();

    /**
     * Validates the syntax of an id.
     * 验证id语发是否错误
     * @param id
     *                the id to validate.
     * @return true if id is well formed.
     */
    boolean isValid(String id);
}
