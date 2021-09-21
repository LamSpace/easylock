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

package io.github.lamtong.easylock.server.metadata;

import io.github.lamtong.easylock.common.core.Request;
import io.github.lamtong.easylock.server.dispatcher.Dispatcher;
import io.netty.channel.ChannelHandlerContext;

/**
 * {@link LockRequestMetaData} is used when locking or unlocking requests are received and resolved. It contains
 * some necessary metadata to resolve requests, lick {@link Request} instance itself, lock type of current
 * {@link Request} instance, whether current request is a lock request or not, if current request is a lock
 * request, whether it is a {@code tryLock} request or not. And most importantly, an instance of type
 * {@link ChannelHandlerContext} is included to send back corresponding response to clients.
 *
 * @author Lam Tong
 * @version 1.3.0
 * @see Dispatcher
 * @since 1.3.0
 */
public final class LockRequestMetaData {

    private final ChannelHandlerContext ctx;

    private final Request request;

    public LockRequestMetaData(ChannelHandlerContext ctx, Request request) {
        this.ctx = ctx;
        this.request = request;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public Request getRequest() {
        return request;
    }

    @Override
    public String toString() {
        return "RequestMetaData{" +
                "ctx=" + ctx +
                ", request=" + request +
                '}';
    }

}
