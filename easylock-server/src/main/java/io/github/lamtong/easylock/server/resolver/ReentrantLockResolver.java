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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
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
 * @version 1.3.0
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
        Request request = this.lockHolder.putIfAbsent(key, lockRequest);
        if (request == null) {
            this.lockCounter.putIfAbsent(key, new AtomicInteger());
            this.lockCounter.get(key).incrementAndGet();
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, acquireLock(lockRequest));
            }
            return new Response(key, lockRequest.getIdentity(), true, SUCCEED, true);
        }
        //noinspection LoopConditionNotUpdatedInsideLoop,StatementWithEmptyBody
        while (!this.lockHolder.containsKey(key)) {
            // Waiting until lock has been acquired.
        }
        request = this.lockHolder.get(key);
        if (request.getIdentity() != lockRequest.getIdentity()) {
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, String.format("[%s] - [%s] acquires ReentrantLock unsuccessfully, current lock is hold by [%s] - [%s].",
                        lockRequest.getApplication(), lockRequest.getThread(),
                        request.getApplication(), request.getThread()));
            }
            return new Response(key, lockRequest.getIdentity(), false, LOCKED_ALREADY, true);
        }
        this.lockHolder.put(key, lockRequest);
        this.lockCounter.get(key).incrementAndGet();
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, acquireLock(lockRequest));
        }
        return new Response(key, lockRequest.getIdentity(), true, SUCCEED, true);
    }

    @Override
    @SuppressWarnings(value = {"Duplicates"})
    public Response resolveLock(Request lockRequest) {
        String key = lockRequest.getKey();
        Request request = this.lockHolder.putIfAbsent(key, lockRequest);
        if (request == null) {
            this.lockCounter.putIfAbsent(key, new AtomicInteger());
            this.lockCounter.get(key).incrementAndGet();
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, acquireLock(lockRequest));
            }
            return new Response(key, lockRequest.getIdentity(), true, SUCCEED, true);
        }
        //noinspection LoopConditionNotUpdatedInsideLoop,StatementWithEmptyBody
        while (!this.lockHolder.containsKey(key)) {
            // Waiting until lock has been acquired.
        }
        request = this.lockHolder.get(key);
        if (request.getIdentity() == lockRequest.getIdentity()) {
            this.lockHolder.put(key, lockRequest);
            this.lockCounter.get(key).incrementAndGet();
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, acquireLock(lockRequest));
            }
            return new Response(key, lockRequest.getIdentity(), true, SUCCEED, true);
        }
        this.requests.putIfAbsent(key, new LinkedBlockingQueue<>());
        this.permissions.putIfAbsent(key, new SynchronousQueue<>());
        try {
            this.requests.get(key).put(new Object());
            this.permissions.get(key).take();
            this.lockHolder.put(key, lockRequest);
            this.lockCounter.putIfAbsent(key, new AtomicInteger());
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
        return new Response(key, lockRequest.getIdentity(), true, SUCCEED, true);
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
                if (this.requests.containsKey(key)) {
                    if (!this.requests.get(key).isEmpty()) {
                        this.requests.get(key).take();
                        this.permissions.get(key).put(new Object());
                    } else {
                        this.requests.remove(key);
                        this.permissions.remove(key);
                    }
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
        return new Response(key, unlockRequest.getIdentity(), true, SUCCEED, false);
    }

    @Override
    public String acquireLock(Request request) {
        return String.format("[%s] - [%s] acquires ReentrantLock successfully, current lock number: %s.",
                request.getApplication(), request.getThread(), this.lockCounter.get(request.getKey()).get());
    }

    @Override
    public String releaseLock(Request request) {
        return String.format("[%s] - [%s] releases ReentrantLock successfully, current lock number: %s.",
                request.getApplication(), request.getThread(), this.lockCounter.get(request.getKey()).get());
    }

    @Override
    public boolean isLocked(Request request) {
        String key = request.getKey();
        if (!this.lockHolder.containsKey(key)) {
            return false;
        }
        Request lockRequest = this.lockHolder.get(key);
        return request.getIdentity() == lockRequest.getIdentity();
    }

    public String releaseLockCompletely(Request request) {
        return String.format("[%s] - [%s] releases ReentrantLock completely.",
                request.getApplication(), request.getThread());
    }

}
