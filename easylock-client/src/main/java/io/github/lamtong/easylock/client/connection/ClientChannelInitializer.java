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

import io.github.lamtong.easylock.common.core.Response;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

/**
 * Initializer for each client's channel when channels are initialized to server. Each channel
 * requires one and only one {@link ClientChannelInitializer}.
 * <p>
 * Be aware that {@link ClientChannelInitializer} initialize the channel from client to server,
 * which means that it not only sends requests to server, but also receives responses from server.
 * Hence {@link ClientChannelInitializer} requires an instance of {@link ClientHandler} to
 * handle responses from server.
 *
 * @author Lam Tong
 * @version 1.3.2
 * @see ClientChannelPoolHandler
 * @see ClientHandler
 * @since 1.0.0
 */
public final class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final ClientHandler handler;

    public ClientChannelInitializer(ClientHandler handler) {
        this.handler = handler;
    }

    /**
     * Initials each channel using basic handler provided by <b>Netty</b>.
     *
     * @param socketChannel socket channel
     */
    @Override
    protected void initChannel(SocketChannel socketChannel) {
        socketChannel.pipeline()
                .addLast(new ProtobufVarint32FrameDecoder())
                .addLast(new ProtobufDecoder(Response.ResponseProto.getDefaultInstance()))
                .addLast(new ProtobufVarint32LengthFieldPrepender())
                .addLast(new ProtobufEncoder())
                .addLast(this.handler);
    }

}
