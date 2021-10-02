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
import io.github.lamtong.easylock.common.core.Response;
import io.github.lamtong.easylock.server.metadata.LockRequestMetaData;
import io.github.lamtong.easylock.server.pipeline.Pipeline;
import io.github.lamtong.easylock.server.resolver.*;
import io.netty.channel.ChannelHandlerContext;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Default implementation of {@link Dispatcher} to dispatch requests. At previous released versions, namely
 * versions before 1.3.0, parallel requests are resolved parallel using a thread pool, which may incur lack of
 * limited resources. But now things has been changed, and parallel requests are resolved serially. More precisely,
 * requests with {@code lock()} operation will be added into a {@link Queue} and then resolved serially while
 * requests with {@code tryLock()} operation and {@code unlock()} will also be resolved parallel cause these
 * requests need to be resolved and returned immediately without blocking.
 * <p>
 * <b>Implementation of Serialization</b>
 * <p>
 * When requests with {@code lock()} operations will be passed into an instance of type {@link Pipeline}
 * via request's {@link Request#type} by wrapping into an instance of type {@link LockRequestMetaData}.
 *
 * @author Lam Tong
 * @version 1.3.1
 * @see Dispatcher
 * @see SimpleLockResolver
 * @see TimeoutLockResolver
 * @see ReentrantLockResolver
 * @see ReadWriteLockResolver
 * @see Pipeline
 * @since 1.3.0
 */
public final class DefaultDispatcher implements Dispatcher {

    /**
     * Singleton of {@link DefaultDispatcher}.
     */
    private static final DefaultDispatcher instance = new DefaultDispatcher();

    private static final ExecutorService pool = Executors.newCachedThreadPool();

    private final SimpleLockResolver simpleLock = SimpleLockResolver.getResolver();

    private final TimeoutLockResolver timeoutLock = TimeoutLockResolver.getResolver();

    private final ReentrantLockResolver reentrantLock = ReentrantLockResolver.getResolver();

    private final ReadWriteLockResolver readWriteLock = ReadWriteLockResolver.getResolver();

    private final Pipeline simpleLockPipeline = Pipeline.getSimpleLockPipeline();

    private final Pipeline timeoutLockHPipeline = Pipeline.getTimeoutLockPipeline();

    private final Pipeline reentrantLockPipeline = Pipeline.getReentrantLockPipeline();

    private final Pipeline readWriteLockPipeline = Pipeline.getReadWriteLockPipeline();

    private DefaultDispatcher() {
    }

    public static DefaultDispatcher getInstance() {
        return instance;
    }

    /**
     * Dispatches received requests by overridden method {@link #dispatch(ChannelHandlerContext, Request.RequestProto,
     * AbstractLockResolver, Pipeline)}.
     *
     * @param context channel handler context.
     * @param request request instance to be resolved.
     */
    @Override
    public void dispatch(ChannelHandlerContext context, Request.RequestProto request) {
        switch (request.getType()) {
            case 2:
                this.dispatch(context, request, timeoutLock, timeoutLockHPipeline);
                break;
            case 4:
                this.dispatch(context, request, reentrantLock, reentrantLockPipeline);
                break;
            case 8:
                this.dispatch(context, request, readWriteLock, readWriteLockPipeline);
                break;
            default:
                this.dispatch(context, request, simpleLock, simpleLockPipeline);
        }
    }

    /**
     * Dispatches received requests. Usually, if current request is an unlocking request or a locking
     * request with {@code try-lock()} operation, then it should be resolved immediately. Otherwise,
     * that request should be put into an {@link BlockingQueue} and resolved by only one thread for a certain
     * type of locking requests with the same lock key.
     *
     * @param context  channel handler context.
     * @param request  request to be resolved.
     * @param resolver resolver to resolve unlocking request or locking request with {@code try-lock()}.
     * @param pipeline pipeline to resolve locking request with {@code lock()}, which requires waiting when lock
     *                 resource is not available.
     */
    private void dispatch(ChannelHandlerContext context, Request.RequestProto request,
                          AbstractLockResolver resolver, Pipeline pipeline) {
        if (this.requestResolveImmediately(request)) {
            pool.execute(()->{
                Response.ResponseProto response = resolver.resolve(request);
                context.writeAndFlush(response);
            });
        } else {
            pipeline.put(new LockRequestMetaData(context, request));
        }
    }

    /**
     * If current request instance is an unlocking request or a try-lock request, then returns true, indicating
     * that current request should be resolved immediately.
     *
     * @param request request to be resolved.
     * @return true if and only if current request instance is an unlocking request or a try-lock request; otherwise,
     * returns false.
     */
    private boolean requestResolveImmediately(Request.RequestProto request) {
        return !request.getLockRequest() || request.getTryLock();
    }

}
