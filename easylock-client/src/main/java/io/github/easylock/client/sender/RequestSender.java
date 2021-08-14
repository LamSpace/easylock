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

package io.github.easylock.client.sender;

import io.github.easylock.client.cache.ResponseCache;
import io.github.easylock.client.provider.ChannelPoolProvider;
import io.github.easylock.common.request.LockRequest;
import io.github.easylock.common.request.Request;
import io.github.easylock.common.request.UnlockRequest;
import io.github.easylock.common.response.LockResponse;
import io.github.easylock.common.response.Response;
import io.github.easylock.common.response.UnlockResponse;
import io.netty.channel.Channel;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.util.concurrent.FutureListener;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link RequestSender} sends requests, namely {@link LockRequest} and {@link UnlockRequest},
 * to server and try to acquire corresponding response in {@link ResponseCache}.
 * <p>
 * <b>Usage of {@link Cloneable}</b>
 * <p>
 * Interface {@link Cloneable} indicates that clients can acquire an instance of {@link RequestSender}
 * by cloning the <code>single</code> instance provided by {@link RequestSender} itself, not by
 * constructor of {@link RequestSender}. For example, {@link RequestSender} may be used as belows.
 * <pre>
 *     {@code
 *     ...
 *     RequestSender sender = RequestSender.getSender().clone();
 *     sender.send(request);
 *     ...
 *     }
 * </pre>
 *
 * @author Lam Tong
 * @version 1.0.0
 * @see Cloneable
 * @see Request
 * @see Response
 * @since 1.0.0
 */
public final class RequestSender implements Cloneable {

    private static final Logger logger = Logger.getLogger(RequestSender.class.getName());

    private static final RequestSender sender = new RequestSender();

    private RequestSender() {
    }

    public static RequestSender getSender() {
        return sender;
    }

    @SuppressWarnings("all")
    public Response send(Request request) {
        final String key = request.getKey();
        final int identity = request.getIdentity();
        final FixedChannelPool pool = ChannelPoolProvider.getPool();
        final ResponseCache cache = ResponseCache.getCache();
        pool.acquire().addListener((FutureListener<Channel>) f -> {
            if (f.isSuccess()) {
                // Acquires a channel successfully, then send request and release the channel.
                Channel channel = f.getNow();
                channel.writeAndFlush(request);
                pool.release(channel);
            } else {
                // Fails to acquire a channel, maybe the client fails to connect to server, or network breaddown.
                // Thus requests cancel and responses are created at client to answer the requests.
                if (request instanceof LockRequest) {
                    cache.put(new LockResponse(key, identity, false,
                            "Connection to server fails, lock request cancelled"));
                } else {
                    cache.put(new UnlockResponse(key, identity, false,
                            "Connection to server fails, unlock request cancelled"));
                }
            }
        });
        // After sending the request successfully, current thread will try to retrieve corresponding
        // response from {@link ResponseCache} in pooling if the request is resolved at server and
        // corresponding response arrives and is stored in {@link ResponseCache}.
        //
        //     1.Current thread will check that if there exist a response whose key is the same as
        //       that of the request sends before, and if does, then go to the next step; otherwise,
        //       continue current step.
        //     2.Current thread will check the identity of the received response with that of the
        //       the request sends before. If there exists a response whose identity is the same as
        //       the request sends before, then current response in {@link ResponseCache} can be
        //       acquired with specified key and returned.
        //
        Response response;
        for (; ; ) {
            Response res;
            while ((res = cache.peek(key)) == null) ;
            if (res.getIdentity() == identity) {
                response = cache.take(key);
                break;
            }
        }
        return response;
    }

    /**
     * Clones an instance of type {@link RequestSender}.
     *
     * @return an instance of {@link RequestSender}.
     */
    @Override
    public RequestSender clone() {
        try {
            return ((RequestSender) super.clone());
        } catch (CloneNotSupportedException e) {
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, e.getMessage());
            }
        }
        return null;
    }

}
