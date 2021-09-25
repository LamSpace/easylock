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

package io.github.lamtong.easylock.server.handler;

import io.github.lamtong.easylock.common.core.Request;
import io.github.lamtong.easylock.server.dispatcher.DefaultDispatcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link ServerHandler} is a channel in-bound handler to activate or inactivate channels, receive message
 * from clients and so on. When {@link ServerHandler} receives a message from clients, it dispatches that
 * message to {@link DefaultDispatcher} to resolve the lock or unlock requests.
 *
 * @author Lam Tong
 * @version 1.3.1
 * @see ChannelInboundHandlerAdapter
 * @see DefaultDispatcher
 * @since 1.0.0
 */
public final class ServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = Logger.getLogger(ServerHandler.class.getName());

    private static final DefaultDispatcher dispatcher = DefaultDispatcher.getInstance();

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (logger.isLoggable(Level.SEVERE)) {
            logger.log(Level.SEVERE, "A channel from client [{0}] has inactivated, channel disconnects.",
                    ctx.channel().remoteAddress());
        }
        ctx.channel().close();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "Server has activated a channel. Client address: {0}",
                    ctx.channel().remoteAddress());
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Request.RequestProto request = (Request.RequestProto) msg;
        dispatcher.dispatch(ctx, request);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (logger.isLoggable(Level.SEVERE)) {
            logger.log(Level.SEVERE, "Exception occurs, caused by {0}", cause.getMessage());
        }
        ctx.channel().close();
    }

}
