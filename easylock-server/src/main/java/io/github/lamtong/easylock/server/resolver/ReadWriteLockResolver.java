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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of {@link AbstractLockResolver} for {@code ReadWriteLock}, and I do suppose that it
 * is the most difficult one till now.
 * <p>
 * <b>Implementation Consideration</b>
 * <p>
 * As the names says, {@link ReadWriteLockResolver} resolves locking and unlocking requests for
 * {@code ReadWriteLock}, consisting of a {@code ReadLock} and a {@code WriteLock}. There are differences
 * among both.
 * <ol>
 *     <li>{@code ReadLock} is inclusive, which implies that only threads that hold a read lock of a certain
 *     {@code ReadWriteLock} resource can access shared resources together without modifying that shared
 *     resources.</li>
 *     <li>{@code WriteLock} is exclusive, inferring that only one thread can acquire a {@code WriteLock}
 *     successfully and access shared resources. Any other threads will be blocked until that lock resource
 *     is available.</li>
 *     <li>When multiple threads acquire and hold a {@code ReadLock} resource from a {@code ReadWriteLock}
 *     respectively, any invocation of {@code WriteLock} of that {@code ReadWriteLock} will be blocked until
 *     all read clocks are released successfully.</li>
 * </ol>
 * Instance variables in {@link AbstractLockResolver} are considered to be used to resolve locking and unlocking
 * for {@code WriteLock}. Therefore, extra variables should be claimed for {@code ReadLock}.
 * <p>
 * <b>Support for Downgrade</b>
 * <p>
 * {@code ReadWriteLock} does support that write lock can downgrade to read lock. If current thread acquire a
 * {@code WriteLock}, invocation of locking for {@code ReadLock} will be blocked until that {@code WriteLock}
 * is released except for that current thread can acquire a {@code ReadLock}. Namely, current thread hold a
 * {@code WriteLock} and a {@code ReadLock} at the same time. If current thread releases the {@code WriteLock}
 * first and the {@code ReadLock} at next, then it can be saying that {@code WriteLock} downgrades to
 * {@code ReadLock}.
 * <p>
 * Note that {@code ReadWriteLock} does not support that {@code ReadLock} upgrades to {@code WriteLock}.
 * <p>
 * <b>No Support for Reentrant Usage</b>
 * <p>
 * {@code ReadWriteLock} does not support for reentrant usage, inferring that any threads can only acquire a
 * {@code ReadLock} or a {@code WriteLock} once. Any invocation trying to lock again will fail.
 *
 * @author Lam Tong
 * @version 1.2.0
 * @see AbstractLockResolver
 * @since 1.2.0
 */
public final class ReadWriteLockResolver extends AbstractLockResolver {

    private static final String WL_EXISTS_RL_FAIL = "Locked by a write lock, read locking fails.";

    private static final String WL_EXISTS_WL_FAIL = "Locked by a write lock, write locking fails.";

    private static final String RL_EXISTS_WL_FAIL = "Locked by a read lock, write locking fails.";

    private static final Logger logger = Logger.getLogger(ReadWriteLockResolver.class.getName());

    private static final ReadWriteLockResolver resolver = new ReadWriteLockResolver();

    private final ConcurrentHashMap<String, AtomicInteger> readLockHolder = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, BlockingQueue<Object>> readLockRequests = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, BlockingQueue<Object>> readLockPermissions = new ConcurrentHashMap<>();

    private ReadWriteLockResolver() {
    }

    public static ReadWriteLockResolver getResolver() {
        return resolver;
    }

    @Override
    public Response resolveTryLock(Request lockRequest) {
        if (lockRequest.isReadLock()) {
            return this.resolveReadTryLock(lockRequest);
        } else {
            return this.resolveWriteTryLock(lockRequest);
        }
    }

    @SuppressWarnings(value = {"Duplicates"})
    private Response resolveReadTryLock(Request request) {
        String key = request.getKey();
        if (!this.lockHolder.containsKey(key)) {
            synchronized (this.lockMonitor) {
                if (!this.lockHolder.containsKey(key)) {
                    this.readLockHolder.computeIfAbsent(key, k -> new AtomicInteger());
                    this.readLockHolder.get(key).incrementAndGet();
                    if (logger.isLoggable(Level.INFO)) {
                        logger.log(Level.INFO, this.acquireLock(request));
                    }
                    return new Response(key, request.getIdentity(), true, SUCCEED, true);
                }
            }
        }
        Request writeLockRequest = this.lockHolder.get(key);
        // A write lock downgrades to a read lock.
        if (request.getApplication().equals(writeLockRequest.getApplication()) &&
                request.getThread().equals(writeLockRequest.getThread())) {
            this.readLockHolder.computeIfAbsent(key, k -> new AtomicInteger());
            this.readLockHolder.get(key).incrementAndGet();
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, this.downgrade(request));
            }
            return new Response(key, request.getIdentity(), true, SUCCEED, true);
        }
        return new Response(key, request.getIdentity(), false, WL_EXISTS_RL_FAIL, true);
    }

    @SuppressWarnings(value = {"Duplicates"})
    private Response resolveWriteTryLock(Request request) {
        String key = request.getKey();
        if (!this.lockHolder.containsKey(key) && !this.readLockHolder.containsKey(key)) {
            synchronized (this.lockMonitor) {
                if (!this.lockHolder.containsKey(key) && !this.readLockHolder.containsKey(key)) {
                    this.lockHolder.put(key, request);
                    if (logger.isLoggable(Level.INFO)) {
                        logger.log(Level.INFO, this.acquireLock(request));
                    }
                    return new Response(key, request.getIdentity(), true, SUCCEED, true);
                }
            }
        }
        if (this.lockHolder.containsKey(key)) {
            return new Response(key, request.getIdentity(), false, WL_EXISTS_WL_FAIL, true);
        }
        return new Response(key, request.getIdentity(), false, RL_EXISTS_WL_FAIL, true);
    }

    @Override
    public Response resolveLock(Request lockRequest) {
        if (lockRequest.isReadLock()) {
            return this.resolveReadLock(lockRequest);
        } else {
            return this.resolveWriteLock(lockRequest);
        }
    }

    @SuppressWarnings(value = {"Duplicates"})
    private Response resolveReadLock(Request request) {
        String key = request.getKey();
        if (!this.lockHolder.containsKey(key)) {
            synchronized (this.lockMonitor) {
                if (!this.lockHolder.containsKey(key)) {
                    // If and only if no write lock exists, then read lock succeeds.
                    this.readLockHolder.computeIfAbsent(key, k -> new AtomicInteger());
                    this.readLockHolder.get(key).incrementAndGet();
                    if (logger.isLoggable(Level.INFO)) {
                        logger.log(Level.INFO, acquireLock(request));
                    }
                    return new Response(key, request.getIdentity(), true, SUCCEED, true);
                }
            }
        }
        Request writeLockRequest = this.lockHolder.get(key);
        if (request.getApplication().equals(writeLockRequest.getApplication()) &&
                request.getThread().equals(writeLockRequest.getThread())) {
            // A write lock downgrades to a read lock.
            this.readLockHolder.computeIfAbsent(key, k -> new AtomicInteger());
            this.readLockHolder.get(key).incrementAndGet();
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, downgrade(request));
            }
            return new Response(key, request.getIdentity(), true, SUCCEED, true);
        }
        this.readLockRequests.computeIfAbsent(key, k -> new LinkedBlockingQueue<>());
        this.readLockPermissions.computeIfAbsent(key, k -> new ArrayBlockingQueue<>(1));
        // Demotion of WriteLock fails, then locking request of ReadLock is waiting until permitted.
        try {
            this.readLockRequests.get(key).put(new Object());
            this.readLockPermissions.get(key).take();
            this.readLockHolder.computeIfAbsent(key, k -> new AtomicInteger());
            this.readLockHolder.get(key).incrementAndGet();
        } catch (InterruptedException e) {
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, e.getMessage());
            }
            Thread.currentThread().interrupt();
        }
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, acquireLock(request));
        }
        return new Response(key, request.getIdentity(), true, SUCCEED, true);
    }

    @SuppressWarnings(value = {"Duplicates"})
    private Response resolveWriteLock(Request request) {
        String key = request.getKey();
        if (!this.lockHolder.containsKey(key) && !this.readLockHolder.containsKey(key)) {
            synchronized (this.lockMonitor) {
                if (!this.lockHolder.containsKey(key) && !this.readLockHolder.containsKey(key)) {
                    this.lockHolder.put(key, request);
                    if (logger.isLoggable(Level.INFO)) {
                        logger.log(Level.INFO, acquireLock(request));
                    }
                    return new Response(key, request.getIdentity(), true, SUCCEED, true);
                }
            }
        }
        this.requests.computeIfAbsent(key, k -> new LinkedBlockingQueue<>());
        this.permissions.computeIfAbsent(key, k -> new ArrayBlockingQueue<>(1));
        try {
            this.requests.get(key).put(new Object());
            this.permissions.get(key).take();
            this.lockHolder.put(key, request);
        } catch (InterruptedException e) {
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, e.getMessage());
            }
            Thread.currentThread().interrupt();
        }
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, acquireLock(request));
        }
        return new Response(key, request.getIdentity(), true, SUCCEED, true);
    }

    @Override
    public Response resolveUnlock(Request unlockRequest) {
        if (unlockRequest.isReadLock()) {
            return this.resolveReadUnlock(unlockRequest);
        } else {
            return this.resolveWriteUnlock(unlockRequest);
        }
    }

    @SuppressWarnings(value = {"Duplicates"})
    private Response resolveReadUnlock(Request request) {
        String key = request.getKey();
        int count = this.readLockHolder.get(key).get();
        if (count == 1) {
            this.readLockHolder.remove(key);
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, this.releaseReadLockCompletely(request));
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
            // If there still exists read locks, then release one and mark.
            this.readLockHolder.get(key).decrementAndGet();
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, this.releaseLock(request));
            }
        }
        return new Response(key, request.getIdentity(), true, SUCCEED, false);
    }

    private Response resolveWriteUnlock(Request request) {
        String key = request.getKey();
        this.lockHolder.remove(key);
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, this.releaseLock(request));
        }
        // When a WriteLock is released, if there exists read locking requests, then handle first.
        if (this.readLockRequests.containsKey(key)) {
            try {
                while (this.readLockRequests.get(key).peek() != null) {
                    this.readLockRequests.get(key).take();
                    this.readLockPermissions.get(key).put(new Object());
                }
            } catch (InterruptedException e) {
                if (logger.isLoggable(Level.SEVERE)) {
                    logger.log(Level.SEVERE, e.getMessage());
                }
                Thread.currentThread().interrupt();
            }
        } else if (this.requests.containsKey(key)) {
            // Permits only one write lock request.
            try {
                if (!this.requests.get(key).isEmpty()) {
                    this.requests.get(key).take();
                    this.permissions.get(key).put(new Object());
                }
            } catch (InterruptedException e) {
                if (logger.isLoggable(Level.SEVERE)) {
                    logger.log(Level.SEVERE, e.getMessage());
                }
            }
        }
        return new Response(key, request.getIdentity(), true, SUCCEED, false);
    }

    @Override
    public String acquireLock(Request request) {
        if (request.isReadLock()) {
            return this.acquireReadLock(request);
        } else {
            return this.acquireWriteLock(request);
        }
    }

    private String acquireReadLock(Request request) {
        return String.format("[%s] - [%s] acquires ReadLock successfully, current ReadLock count: [%s].",
                request.getApplication(), request.getThread(), this.readLockHolder.get(request.getKey()).get());
    }

    private String acquireWriteLock(Request request) {
        return String.format("[%s] - [%s] acquires WriteLock successfully.",
                request.getApplication(), request.getThread());
    }

    private String downgrade(Request request) {
        return String.format("[%s] - [%s] acquires ReadLock successfully, current WriteLock downgrades to ReadLock.",
                request.getApplication(), request.getThread());
    }

    @Override
    public String releaseLock(Request request) {
        if (request.isReadLock()) {
            return this.releaseReadLock(request);
        } else {
            return this.releaseWriteLock(request);
        }
    }

    private String releaseReadLock(Request request) {
        return String.format("[%s] - [%s] releases ReadLock successfully, current ReadLock count: [%s]",
                request.getApplication(), request.getThread(), this.readLockHolder.get(request.getKey()).get());
    }

    private String releaseReadLockCompletely(Request request) {
        return String.format("[%s] - [%s] releases ReadLock completely.", request.getApplication(), request.getThread());
    }

    private String releaseWriteLock(Request request) {
        return String.format("[%s] - [%s] releases WriteLock successfully.", request.getApplication(), request.getThread());
    }

}