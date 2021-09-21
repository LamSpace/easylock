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

import io.github.lamtong.easylock.server.metadata.LockRequestMetaData;
import io.github.lamtong.easylock.server.resolver.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * {@link Pipeline} provides a pipe to store requests, grouping by lock key, in an instance of type
 * {@link BlockingQueue}. Abstract class {@link Pipeline} provides a thread pool to resolve requests
 * with the same lock key in a serial manner.
 *
 * @author Lam Tong
 * @version 1.3.0
 * @see DefaultPipeline
 * @see ReentrantPipeline
 * @see RWPipeline
 * @since 1.3.0
 */
public abstract class Pipeline {

    protected static final ExecutorService pool = Executors.newCachedThreadPool();

    protected static final ConcurrentHashMap<String, BlockingQueue<LockRequestMetaData>> pipelines = new ConcurrentHashMap<>();

    protected final AbstractLockResolver resolver;

    protected Pipeline(AbstractLockResolver resolver) {
        this.resolver = resolver;
    }

    public static Pipeline getSimpleLockPipeline() {
        return new DefaultPipeline(SimpleLockResolver.getResolver());
    }

    public static Pipeline getTimeoutLockPipeline() {
        return new DefaultPipeline(TimeoutLockResolver.getResolver());
    }

    public static Pipeline getReentrantLockPipeline() {
        return new ReentrantPipeline(ReentrantLockResolver.getResolver());
    }

    public static Pipeline getReadWriteLockPipeline() {
        return new RWPipeline(ReadWriteLockResolver.getResolver());
    }

    /**
     * Puts request metadata into pipelines. Different locks should be resolved in different manners.
     *
     * @param metaData metadata to be put.
     */
    public abstract void put(LockRequestMetaData metaData);

}
