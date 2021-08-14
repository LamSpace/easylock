/*
 *  Copyright 2021 the original author, Lam Tong
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.github.easylock.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties to configure <code>easylock</code>.
 *
 * @author Lam Tong
 * @version 1.0.0
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = EasyLockProperties.EASY_LOCK_SERVER_PREFIX)
public class EasyLockProperties {

    public static final String EASY_LOCK_SERVER_PREFIX = "easy-lock";

    private String serverHost = "localhost";

    private int serverPort = 40417;

    private int channelConnections = 2;

    private int cacheQueueSize = 1;

    public String getServerHost() {
        return this.serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public int getServerPort() {
        return this.serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public int getChannelConnections() {
        return this.channelConnections;
    }

    public void setChannelConnections(int channelConnections) {
        this.channelConnections = channelConnections;
    }

    public int getCacheQueueSize() {
        return this.cacheQueueSize;
    }

    public void setCacheQueueSize(int cacheQueueSize) {
        this.cacheQueueSize = cacheQueueSize;
    }

}
