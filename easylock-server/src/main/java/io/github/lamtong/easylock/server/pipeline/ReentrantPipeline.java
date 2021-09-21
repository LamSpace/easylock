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

package io.github.lamtong.easylock.server.pipeline;

import io.github.lamtong.easylock.common.core.Request;
import io.github.lamtong.easylock.common.core.Response;
import io.github.lamtong.easylock.server.metadata.LockRequestMetaData;
import io.github.lamtong.easylock.server.resolver.AbstractLockResolver;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@code Pipeline} to resolve {@code ReentrantLock} with {@code lock()} operations.
 *
 * @author Lam Tong
 * @version 1.3.0
 * @see Pipeline
 * @since 1.3.0
 */
public final class ReentrantPipeline extends Pipeline {

    private static final Logger logger = Logger.getLogger(ReentrantPipeline.class.getName());

    public ReentrantPipeline(AbstractLockResolver resolver) {
        super(resolver);
    }

    @Override
    @SuppressWarnings(value = {"Duplicates"})
    public void put(LockRequestMetaData metaData) {
        String key = metaData.getRequest().getKey();
        BlockingQueue<LockRequestMetaData> queue = pipelines.putIfAbsent(key, new LinkedBlockingQueue<>());
        if (queue == null) {
            pool.execute(() -> {
                for (; ; ) {
                    LockRequestMetaData lockRequestMetaData = null;
                    try {
                        lockRequestMetaData = pipelines.get(key).poll(1000L, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        if (logger.isLoggable(Level.SEVERE)) {
                            logger.log(Level.SEVERE, e.getMessage());
                        }
                        Thread.currentThread().interrupt();
                    }
                    if (lockRequestMetaData != null) {
                        Request request = lockRequestMetaData.getRequest();
                        ChannelHandlerContext ctx = lockRequestMetaData.getCtx();
                        Response response = resolver.resolve(request);
                        ctx.writeAndFlush(response);
                    } else {
                        pipelines.remove(key);
                        break;
                    }
                }
            });
        }
        this.put(key, metaData);
    }

    /**
     * Puts lock request metadata into pipelines if and only if that lock has not been acquired successfully
     * before; otherwise, directly resolve that lock.
     *
     * @param key      lock key
     * @param metaData metadata to be put
     */
    private void put(String key, LockRequestMetaData metaData) {
        Request request = metaData.getRequest();
        if (this.resolver.isLocked(request)) {
            ChannelHandlerContext ctx = metaData.getCtx();
            Response response = this.resolver.resolve(request);
            ctx.writeAndFlush(response);
        } else {
            try {
                pipelines.get(key).put(metaData);
            } catch (InterruptedException e) {
                if (logger.isLoggable(Level.SEVERE)) {
                    logger.log(Level.SEVERE, e.getMessage());
                }
                Thread.currentThread().interrupt();
            }
        }
    }

}
