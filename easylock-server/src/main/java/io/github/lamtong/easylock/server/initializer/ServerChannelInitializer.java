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

package io.github.lamtong.easylock.server.initializer;

import io.github.lamtong.easylock.server.handler.ServerHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

/**
 * {@link ServerChannelInitializer} initializes the server, which extends {@link ChannelInitializer}
 * to provide enhanced functionalities of {@link SocketChannel}. When initializing,
 * {@link ServerChannelInitializer} adds various {@link ChannelHandler}s for encoding, decoding and
 * other functionalities like {@link ServerHandler}, which tries to resolve received messages from clients.
 *
 * @author Lam Tong
 * @version 1.0.0
 * @see ChannelInitializer
 * @since 1.0.0
 */
public final class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel socketChannel) {
        socketChannel.pipeline()
                .addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,
                        0, 4,
                        0, 4))
                .addLast(new LengthFieldPrepender(4))
                .addLast("encoder", new ObjectEncoder())
                .addLast("decoder", new ObjectDecoder(Integer.MAX_VALUE,
                        ClassResolvers.cacheDisabled(null)))
                .addLast(new ServerHandler());
    }

}
