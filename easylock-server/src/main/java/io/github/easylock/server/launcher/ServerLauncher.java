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

package io.github.easylock.server.launcher;

import io.github.easylock.server.initializer.ServerChannelInitializer;
import io.github.easylock.server.property.Properties;
import io.github.easylock.server.property.ServerProperties;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link ServerLauncher} launches the lock server via <code>Netty</code> with {@link
 * ServerProperties}. <b>Be aware</b> that when launch thread will block when server's channel waits to
 * close.
 *
 * @author Lam Tong
 * @version 1.0.0
 * @see ServerChannelInitializer
 * @since 1.0.0
 */
public final class ServerLauncher implements Launcher {

    private static final Logger logger = Logger.getLogger(ServerLauncher.class.getName());

    @Override
    public void launch(Properties properties) {
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "Starts to launch easylock server.");
        }
        ServerProperties serverProperties = (ServerProperties) properties;
        EventLoopGroup boss = new NioEventLoopGroup();
        EventLoopGroup worker = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, serverProperties.getBacklog())
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ServerChannelInitializer());
            ChannelFuture future = bootstrap.bind(serverProperties.getPort()).sync();
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "Easylock server is launched, listening at port {0}", serverProperties.getPort());
            }
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, e.getMessage());
            }
            Thread.currentThread().interrupt();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

}
