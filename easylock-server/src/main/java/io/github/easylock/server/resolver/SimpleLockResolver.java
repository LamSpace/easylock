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

package io.github.easylock.server.resolver;

import io.github.easylock.common.request.LockRequest;
import io.github.easylock.common.request.Request;
import io.github.easylock.common.request.UnlockRequest;
import io.github.easylock.common.response.LockResponse;
import io.github.easylock.common.response.UnlockResponse;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link SimpleLockResolver} extends {@link AbstractLockResolver} and overrides those abstract methods
 * to fulfill the process for {@link LockRequest} and {@link UnlockRequest} due to that
 * {@link AbstractLockResolver#resolve(Request)} has already define a template to resolve requests.
 *
 * @author Lam Tong
 * @version 1.0.0
 * @see AbstractLockResolver
 * @since 1.0.0
 */
public final class SimpleLockResolver extends AbstractLockResolver {

    private static final Logger logger = Logger.getLogger(SimpleLockResolver.class.getName());

    private static volatile SimpleLockResolver resolver = null;

    private SimpleLockResolver() {
    }

    public static SimpleLockResolver getResolver() {
        if (resolver == null) {
            synchronized (SimpleLockResolver.class) {
                if (resolver == null) {
                    resolver = new SimpleLockResolver();
                }
            }
        }
        return resolver;
    }

    @Override
    public LockResponse resolveTryLock(LockRequest lockRequest) {
        String key = lockRequest.getKey();
        if (!this.lockHolder.containsKey(key)) {
            synchronized (this.lockMonitor) {
                if (!this.lockHolder.containsKey(key)) {
                    this.lockHolder.put(key, lockRequest);
                    if (logger.isLoggable(Level.INFO)) {
                        logger.log(Level.INFO, "[" + lockRequest.getApplicationName() + "] - [" +
                                lockRequest.getThreadName() + "] acquires SimpleLock successfully.");
                    }
                    return new LockResponse(key, lockRequest.getIdentity(), true, SUCCEED);
                }
            }
        }
        return new LockResponse(key, lockRequest.getIdentity(), false, LOCKED_ALREADY);
    }

    @Override
    public LockResponse resolveLock(LockRequest lockRequest) {
        String key = lockRequest.getKey();
        if (!this.lockHolder.containsKey(key)) {
            synchronized (this.lockMonitor) {
                if (!this.lockHolder.containsKey(key)) {
                    this.lockHolder.put(key, lockRequest);
                    if (logger.isLoggable(Level.INFO)) {
                        logger.log(Level.INFO, "[" + lockRequest.getApplicationName() + "] - [" +
                                lockRequest.getThreadName() + "] acquires SimpleLock successfully.");
                    }
                    return new LockResponse(key, lockRequest.getIdentity(),
                            true, SUCCEED);
                }
            }
        }
        this.requests.computeIfAbsent(key, k -> new LinkedBlockingQueue<>());
        this.permissions.computeIfAbsent(key, k -> new ArrayBlockingQueue<>(1));
        try {
            this.requests.get(key).put(new Object());
            this.permissions.get(key).take();
            this.lockHolder.put(key, lockRequest);
        } catch (InterruptedException e) {
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, e.getMessage());
            }
        }
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "[" + lockRequest.getApplicationName() + "] - [" +
                    lockRequest.getThreadName() + "] acquires SimpleLock successfully.");
        }
        return new LockResponse(key, lockRequest.getIdentity(),
                true, SUCCEED);
    }

    @Override
    public UnlockResponse resolveUnlock(UnlockRequest unlockRequest) {
        String key = unlockRequest.getKey();
        this.lockHolder.remove(key);
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "[" + unlockRequest.getApplicationName() + "] - [" +
                    unlockRequest.getThreadName() + "] releases SimpleLock successfully.");
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
        }
        return new UnlockResponse(key, unlockRequest.getIdentity(),
                true, SUCCEED);
    }

}
