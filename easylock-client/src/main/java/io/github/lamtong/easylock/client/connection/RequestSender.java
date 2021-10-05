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

import io.github.lamtong.easylock.common.core.Request;
import io.github.lamtong.easylock.common.core.Response;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.util.concurrent.FutureListener;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link RequestSender} sends requests, namely {@code Lock Request} and {@code Unlock Request},
 * to server and try to acquire corresponding response in {@link ResponsePool}.
 *
 * @author Lam Tong
 * @version 1.3.2
 * @see Request
 * @see Response
 * @since 1.0.0
 */
public final class RequestSender {

    private static final Logger logger = Logger.getLogger(RequestSender.class.getName());

    private static final RequestSender sender = new RequestSender();

    /**
     * A blocking queue for flow-limitation with fixed capacity. When huge amount of threads try to send requests
     * asynchronously, all of these threads will try to acquire limited threads resource, provided by {@link EventLoopGroup}.
     * Since that threads of {@link EventLoopGroup} are in charge of connections to server, sending requests to server
     * and receiving response from server. Generally, if client sends a request in one thread, it will receive a
     * response in another thread. In order to avoid starvation of threads to receive responses, flow-limitation
     * is used to control number of threads to send requests.
     * <p>
     * Blocking queue with fixed size, namely {@link ArrayBlockingQueue} is used to control the flow. Generally,
     * it's recommended that size of {@link ArrayBlockingQueue} should be half of {@link NioEventLoopGroup#executorCount()}
     * while it still can be modified.
     */
    private final BlockingQueue<Object> queue = new ArrayBlockingQueue<>(ChannelPoolProvider.getIOThreads());

    private RequestSender() {
    }

    public static RequestSender getSender() {
        return sender;
    }

    /**
     * Sends a {@link Request} instance to server and retrieves corresponding responding response.
     *
     * @param request {@link Request} instance resolved at server.
     * @return corresponding response.
     */
    public Response.ResponseProto send(Request.RequestProto request) {
        final String key = request.getKey();
        final boolean lockRequest = request.getLockRequest();
        final long identity = request.getIdentity();
        final ResponsePool responsePool = ResponsePool.getInstance();
        final FixedChannelPool channelPool = ChannelPoolProvider.getPool();
        try {
            // If current thread is going to send a request asynchronously, it should acquire a chance.
            this.queue.put(new Object());
        } catch (InterruptedException e) {
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, e.getMessage());
            }
            Thread.currentThread().interrupt();
        }
        // Tries to send the request asynchronously.
        channelPool.acquire().addListener((FutureListener<Channel>) f -> {
            if (f.isSuccess()) {
                // Acquires a channel successfully, then send request and release the channel.
                Channel channel = f.getNow();
                channel.writeAndFlush(request);
                channelPool.release(channel);
            } else {
                // Fails to acquire a channel, maybe the client fails to connect to server, or network breakdown.
                // Thus requests cancel and responses are created at client to answer the requests.
                if (lockRequest) {
                    responsePool.put(Response.ResponseProto.newBuilder()
                            .setKey(key)
                            .setIdentity(identity)
                            .setSuccess(false)
                            .setCause("Connection to server fails, lock request cancelled")
                            .setLockResponse(true)
                            .build());
                } else {
                    responsePool.put(Response.ResponseProto.newBuilder()
                            .setKey(key)
                            .setIdentity(identity)
                            .setSuccess(false)
                            .setCause("Connection to server fails, unlock request cancelled")
                            .setLockResponse(false)
                            .build());
                }
            }
            try {
                // After sends the request, chances for sending request should be put back for other threads.
                queue.take();
            } catch (InterruptedException e) {
                if (logger.isLoggable(Level.SEVERE)) {
                    logger.log(Level.SEVERE, e.getMessage());
                }
                Thread.currentThread().interrupt();
            }
        });
        return responsePool.take(request);
    }

}
