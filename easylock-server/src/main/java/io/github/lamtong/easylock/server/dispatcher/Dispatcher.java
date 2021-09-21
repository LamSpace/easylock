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

package io.github.lamtong.easylock.server.dispatcher;

import io.github.lamtong.easylock.common.core.Request;
import io.netty.channel.ChannelHandlerContext;

/**
 * {@link Dispatcher} is an entrance to resolve locking and unlocking requests.
 *
 * @author Lam Tong
 * @version 1.3.0
 * @see DefaultDispatcher
 * @since 1.3.0
 */
public interface Dispatcher {

    /**
     * Dispatches current request with an instance of type {@link ChannelHandlerContext}, which is used to
     * send back corresponding responses.
     *
     * @param context channel handler context.
     * @param request request instance to be resolved.
     */
    void dispatch(ChannelHandlerContext context, Request request);

}
