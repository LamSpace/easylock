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

package io.github.lamtong.easylock.server.resolver;

import io.github.lamtong.easylock.common.core.Request;
import io.github.lamtong.easylock.common.core.Response;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link ReentrantLockResolver} extends {@link AbstractLockResolver} to resolve {@code Lock Request} and
 * {@code Unlock Request} for {@code ReentrantLock}.
 * <p>
 * <b>Implementation Considerations</b>
 * <p>
 * When handling a lock request, whether {@code tryLock()} or {@code lock()}, lock counter should be used to
 * record lock count. And if locking succeeds, corresponding lock counter increases by one.
 * <p>
 * <b>Caution of Reentrant Usage</b>
 * <p>
 * Note {@link ReentrantLockResolver} support for <code>reentrant usage</code>, which indicates that a thread
 * may acquire this kind of lock resource twice if locking succeeds before. And unlocking operations should
 * be the same as successful locking operations. If unlock operations are less than needed, then this lock
 * resource will be hold forever, and any other threads gain no chance to acquire the lock.
 *
 * @author Lam Tong
 * @version 1.2.0
 * @see AbstractLockResolver
 * @since 1.1.0
 */
public final class ReentrantLockResolver extends AbstractLockResolver {

    private static final Logger logger = Logger.getLogger(ReentrantLockResolver.class.getName());

    private static final ReentrantLockResolver resolver = new ReentrantLockResolver();

    private final ConcurrentHashMap<String, AtomicInteger> lockCounter = new ConcurrentHashMap<>();

    private ReentrantLockResolver() {
    }

    public static ReentrantLockResolver getResolver() {
        return resolver;
    }

    @Override
    @SuppressWarnings(value = {"Duplicates"})
    public Response resolveTryLock(Request lockRequest) {
        String key = lockRequest.getKey();
        if (!this.lockHolder.containsKey(key)) {
            synchronized (this.lockMonitor) {
                if (!this.lockHolder.containsKey(key)) {
                    this.lockHolder.put(key, lockRequest);
                    this.lockCounter.computeIfAbsent(key, k -> new AtomicInteger());
                    this.lockCounter.get(key).incrementAndGet();
                    if (logger.isLoggable(Level.INFO)) {
                        logger.log(Level.INFO, acquireLock(lockRequest));
                    }
                    return new Response(key, lockRequest.getIdentity(), true, SUCCEED,
                            true);
                }
            }
        }
        //noinspection LoopConditionNotUpdatedInsideLoop
        while (!this.lockHolder.containsKey(key)) {
            // Waiting until lock has been acquired.
        }
        Request request = this.lockHolder.get(key);
        if (request.getIdentity() != lockRequest.getIdentity()) {
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "[" + lockRequest.getApplication() + SEPARATOR +
                        lockRequest.getThread() + "] acquires ReentrantLock unsuccessfully, " +
                        "current lock is hold by [" + request.getApplication() + SEPARATOR +
                        request.getThread() + "].");
            }
            return new Response(key, lockRequest.getIdentity(), false, LOCKED_ALREADY,
                    true);
        }
        this.lockHolder.put(key, lockRequest);
        this.lockCounter.get(key).incrementAndGet();
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, acquireLock(lockRequest));
        }
        return new Response(key, lockRequest.getIdentity(), true, SUCCEED,
                true);
    }

    @Override
    @SuppressWarnings(value = {"Duplicates"})
    public Response resolveLock(Request lockRequest) {
        String key = lockRequest.getKey();
        if (!this.lockHolder.containsKey(key)) {
            synchronized (this.lockMonitor) {
                if (!this.lockHolder.containsKey(key)) {
                    this.lockHolder.put(key, lockRequest);
                    this.lockCounter.computeIfAbsent(key, k -> new AtomicInteger());
                    this.lockCounter.get(key).incrementAndGet();
                    if (logger.isLoggable(Level.INFO)) {
                        logger.log(Level.INFO, acquireLock(lockRequest));
                    }
                    return new Response(key, lockRequest.getIdentity(), true, SUCCEED,
                            true);
                }
            }
        }
        //noinspection LoopConditionNotUpdatedInsideLoop
        while (!this.lockHolder.containsKey(key)) {
            // Waiting until lock has been acquired.
        }
        Request request = this.lockHolder.get(key);
        if (request.getIdentity() == lockRequest.getIdentity()) {
            this.lockHolder.put(key, lockRequest);
            this.lockCounter.get(key).incrementAndGet();
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, acquireLock(lockRequest));
            }
            return new Response(key, lockRequest.getIdentity(), true, SUCCEED,
                    true);
        }
        this.requests.computeIfAbsent(key, k -> new LinkedBlockingQueue<>());
        this.permissions.computeIfAbsent(key, k -> new ArrayBlockingQueue<>(1));
        try {
            this.requests.get(key).put(new Object());
            this.permissions.get(key).take();
            this.lockHolder.put(key, lockRequest);
            this.lockCounter.computeIfAbsent(key, k -> new AtomicInteger());
            this.lockCounter.get(key).incrementAndGet();
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, acquireLock(lockRequest));
            }
        } catch (InterruptedException e) {
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, e.getMessage());
            }
            Thread.currentThread().interrupt();
        }
        return new Response(key, lockRequest.getIdentity(), true, SUCCEED,
                true);
    }

    @Override
    @SuppressWarnings(value = {"Duplicates"})
    public Response resolveUnlock(Request unlockRequest) {
        String key = unlockRequest.getKey();
        int cnt = this.lockCounter.get(key).get();
        if (cnt == 1) {
            this.lockCounter.remove(key);
            this.lockHolder.remove(key);
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, releaseLockCompletely(unlockRequest));
            }
            try {
                if (this.requests.containsKey(key) && !this.requests.get(key).isEmpty()) {
                    this.requests.get(key).take();
                    this.permissions.get(key).put(new Object());
                }
            } catch (InterruptedException e) {
                if (logger.isLoggable(Level.SEVERE)) {
                    logger.log(Level.SEVERE, e.getMessage());
                }
                Thread.currentThread().interrupt();
            }
        } else {
            this.lockCounter.get(key).decrementAndGet();
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, releaseLock(unlockRequest));
            }
        }
        return new Response(key, unlockRequest.getIdentity(), true, SUCCEED,
                false);
    }

    @Override
    public String acquireLock(Request request) {
        return "[" + request.getApplication() + SEPARATOR + request.getThread() +
                "] acquires ReentrantLock successfully, current lock count: " +
                this.lockCounter.get(request.getKey()).get() + ".";
    }

    @Override
    public String releaseLock(Request request) {
        return "[" + request.getApplication() + SEPARATOR + request.getThread() +
                "] releases ReentrantLock successfully, current lock count: " +
                this.lockCounter.get(request.getKey()).get() + ".";
    }

    public String releaseLockCompletely(Request request) {
        return "[" + request.getApplication() + SEPARATOR + request.getThread() +
                "] releases ReentrantLock completely.";
    }

}
