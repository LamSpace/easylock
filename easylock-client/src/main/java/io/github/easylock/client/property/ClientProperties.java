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

package io.github.easylock.client.property;

/**
 * Client properties. Some of them may be used for connections to server, and others are
 * used to send lock or unlock requests. Note that <code>singleton</code> pattern is adopted
 * for {@link ClientProperties}.
 *
 * @author Lam Tong
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ClientProperties {

    private static final ClientProperties properties = new ClientProperties();

    private String application = "easylock-client";

    private String host = "localhost";

    private int port = 40417;

    private int connections = 2;

    private int queueSize = 1;

    private ClientProperties() {
    }

    public static ClientProperties getProperties() {
        return properties;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getConnections() {
        return connections;
    }

    public void setConnections(int connections) {
        this.connections = connections;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    @Override
    public String toString() {
        return "ClientProperties{" +
                "application='" + this.application + '\'' +
                ", host='" + this.host + '\'' +
                ", port=" + this.port +
                ", connections=" + this.connections +
                '}';
    }

}
