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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of {@link Pipeline} for {@code ReadWriteLock}. Usually, a {@code ReadWriteLock} contains
 * a {@code ReadLock} and a {@code WriteLock} with the same lock. Therefore, there are two pipelines, one
 * of both resolve WriteLock requests and the other one resolve ReadLock requests.
 *
 * @author Lam Tong
 * @version 1.3.0
 * @see Pipeline
 * @since 1.3.0
 */
public final class RWPipeline extends Pipeline {

    private static final Logger logger = Logger.getLogger(RWPipeline.class.getName());

    private final ConcurrentHashMap<String, BlockingQueue<LockRequestMetaData>> readLockPipelines = new ConcurrentHashMap<>();

    public RWPipeline(AbstractLockResolver resolver) {
        super(resolver);
    }

    @Override
    @SuppressWarnings(value = {"Duplicates"})
    public void put(LockRequestMetaData metaData) {
        Request request = metaData.getRequest();
        String key = request.getKey();
        if (request.isReadLock()) {
            BlockingQueue<LockRequestMetaData> queue = this.readLockPipelines.putIfAbsent(key, new LinkedBlockingQueue<>());
            if (queue == null) {
                pool.execute(() -> {
                    for (; ; ) {
                        LockRequestMetaData lockRequestMetaData = null;
                        try {
                            lockRequestMetaData = readLockPipelines.get(key).poll(1000L, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            if (logger.isLoggable(Level.SEVERE)) {
                                logger.log(Level.SEVERE, e.getMessage());
                            }
                            Thread.currentThread().interrupt();
                        }
                        if (lockRequestMetaData != null) {
                            Request metaDataRequest = lockRequestMetaData.getRequest();
                            ChannelHandlerContext ctx = lockRequestMetaData.getCtx();
                            Response response = resolver.resolve(metaDataRequest);
                            ctx.writeAndFlush(response);
                        } else {
                            readLockPipelines.remove(key);
                            break;
                        }
                    }
                });
            }
            this.putReadRequest(key, metaData);
        } else {
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
                            Request metaDataRequest = lockRequestMetaData.getRequest();
                            ChannelHandlerContext ctx = lockRequestMetaData.getCtx();
                            Response response = resolver.resolve(metaDataRequest);
                            ctx.writeAndFlush(response);
                        } else {
                            pipelines.remove(key);
                            break;
                        }
                    }
                });
            }
            this.putWriteRequest(key, metaData);
        }
    }

    /**
     * Puts read lock request metadata into read lock pipelines.
     *
     * @param key      lock key
     * @param metaData metadata to be put
     */
    private void putReadRequest(String key, LockRequestMetaData metaData) {
        try {
            this.readLockPipelines.get(key).put(metaData);
        } catch (InterruptedException e) {
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, e.getMessage());
            }
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Puts write lock request metadata into default pipelines.
     *
     * @param key      lock key
     * @param metaData metadata to be put
     */
    private void putWriteRequest(String key, LockRequestMetaData metaData) {
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
