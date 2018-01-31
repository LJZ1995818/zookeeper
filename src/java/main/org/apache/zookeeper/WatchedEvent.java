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
package org.apache.zookeeper;

import org.apache.yetus.audience.InterfaceAudience;
import org.apache.zookeeper.proto.WatcherEvent;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;

/**
 *  A WatchedEvent represents a change on the ZooKeeper that a Watcher
 *  is able to respond to.  The WatchedEvent includes exactly what happened,
 *  the current state of the ZooKeeper, and the path of the znode that
 *  was involved in the event.
 * WatchedEvent 代表一个观察者能够响应的ZooKeeper的更改。 
 * WatchedEvent 包括了所发生的事情, ZooKeeper的当前状态, 以及参与事件的 znode 的路径。
 */
@InterfaceAudience.Public
public class WatchedEvent {
    final private KeeperState keeperState;
    final private EventType eventType;
    private String path;

    /**
     * Create a WatchedEvent with specified type, state and path
     */
    public WatchedEvent(EventType eventType, KeeperState keeperState, String path) {
        this.keeperState = keeperState;
        this.eventType = eventType;
        this.path = path;
    }

    /**
     * Convert a WatcherEvent sent over the wire into a full-fledged WatcherEvent
     * 转换 WatcherEvent 发送到一个正式的 WatcherEvent
     */
    public WatchedEvent(WatcherEvent eventMessage) {
        keeperState = KeeperState.fromInt(eventMessage.getState());
        eventType = EventType.fromInt(eventMessage.getType());
        path = eventMessage.getPath();
    }

    public KeeperState getState() {
        return keeperState;
    }

    public EventType getType() {
        return eventType;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "WatchedEvent state:" + keeperState
                + " type:" + eventType + " path:" + path;
    }

    /**
     *  Convert WatchedEvent to type that can be sent over network
     *  将 WatchedEvent 转换为可以通过网络发送的类型
     */
    public WatcherEvent getWrapper() {
        return new WatcherEvent(eventType.getIntValue(),
                keeperState.getIntValue(),
                path);
    }
}
