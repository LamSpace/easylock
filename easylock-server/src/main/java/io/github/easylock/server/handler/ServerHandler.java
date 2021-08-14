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

package io.github.easylock.server.handler;

import io.github.easylock.common.core.Request;
import io.github.easylock.common.core.Response;
import io.github.easylock.common.util.Loggers;
import io.github.easylock.server.resolver.RequestResolver;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link ServerHandler} is a channel in-bound handler to activate or inactivate channels, receive message
 * from clients and so on. When {@link ServerHandler} receives a message from clients, it dispatches that
 * message to {@link RequestResolver} to resolve the lock or unlock request via a thread pool.
 *
 * @author Lam Tong
 * @version 1.0.0
 * @see ChannelInboundHandlerAdapter
 * @see RequestResolver
 * @since 1.0.0
 */
public final class ServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = Logger.getLogger(ServerHandler.class.getName());

    private static final ExecutorService threads = Executors.newCachedThreadPool();

    private static final RequestResolver resolver = new RequestResolver();

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Loggers.log(logger, Level.SEVERE, "A channel from client {" +
                ctx.channel().remoteAddress().toString() + "}" +
                " has inactivated, channel disconnects.");
        ctx.channel().close();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Loggers.log(logger, Level.INFO, "Server has activated a channel. Client address: " +
                ctx.channel().remoteAddress().toString());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Request request = ((Request) msg);
        Loggers.log(logger, Level.INFO, "Server has received request from client ["
                + request.getApplication() + "]-[" + request.getThread() + "].");
        threads.execute(() -> {
            Response response = resolver.resolve(request);
            ctx.writeAndFlush(response);
            Loggers.log(logger, Level.INFO, "Server has acknowledged request from client ["
                    + request.getApplication() + "]-[" + request.getThread() + "].");
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Loggers.log(logger, Level.SEVERE, "Exception occurs, caused by " + cause.getMessage());
        ctx.channel().close();
    }

}
