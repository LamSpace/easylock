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

package io.github.lamtong.easylock.client.connection;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

/**
 * {@link ChannelPoolProvider} provides a channel pool for clients, allowing them to send
 * lock or unlock requests asynchronously using limited channels.
 * <p>
 * By default, {@link ChannelPoolProvider} provides two channels where the number of channels
 * is decided by {@link ClientProperties#connections}. And each channel requires an instance
 * of {@link ClientChannelPoolHandler} to create, release or acquire a channel.
 * <p>
 * Here, logical relationship for client's channels can be described as belows when connections
 * from client to server is two.
 * <pre>
 *     {@code
 *                      FixedChannelPool ( provide a channel pool )
 *                     .                .
 *                    .                  .
 *                   .                    .
 *                  .                      .
 *   ClientChannelPoolHandler        ClientChannelPoolHandler
 *                 .                        .
 *                 .                        . ( create,release,or acquire channel )
 *                 .                        .
 *   ClientChannelInitializer        ClientChannelInitializer
 *                 .                        .
 *                 .                        . ( initialize channel )
 *                 .                        .
 *       ResponseReceiver             ResponseReceiver
 *                  .                      .
 *                   .                    .
 *                    .                  . ( handle response from server )
 *                     .                .
 *                      .              .
 *                       ResponseCache ( store response in cache pool )
 *     }
 * </pre>
 *
 * @author Lam Tong
 * @version 1.2.0
 * @see ClientChannelPoolHandler
 * @see FixedChannelPool
 * @since 1.0.0
 */
public final class ChannelPoolProvider {

    private static final ClientProperties properties = ClientProperties.getProperties();

    private static final EventLoopGroup worker = new NioEventLoopGroup();

    private static final Bootstrap bootstrap = new Bootstrap();

    private static final FixedChannelPool pool;

    static {
        bootstrap.group(worker)
                .remoteAddress(new InetSocketAddress(properties.getHost(), properties.getPort()))
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true);
        pool = new FixedChannelPool(bootstrap, new ClientChannelPoolHandler(),
                properties.getConnections());
    }

    private ChannelPoolProvider() {
    }

    public static FixedChannelPool getPool() {
        return pool;
    }

}
