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

package io.github.easylock.client.lock;

import io.github.easylock.client.error.RequestError;
import io.github.easylock.client.property.ClientProperties;
import io.github.easylock.client.sender.RequestSender;
import io.github.easylock.common.request.LockRequest;
import io.github.easylock.common.request.UnlockRequest;
import io.github.easylock.common.response.Response;
import io.github.easylock.common.type.Type;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link SimpleLock} is an implementation of {@link Lock}.
 * <p>
 * <b>No Support for Repeatable Usage</b>
 * <p>
 * Generally, {@link SimpleLock} is a fundamental implementation of {@link Lock}. Instance of
 * {@link SimpleLock} can only acquires the lock at most once successfully. Namely, if {@link #tryLock()}
 * returns <code>true</code>, then {@link #lock()} returns immediately. Even {@link #unlock()} is
 * invoked successfully and the lock is released, invocation of {@link #lock()} and {@link #tryLock()}
 * never acquires the lock since that lock instance has been consumed already.
 * <p>
 * <b>No Support for Reentrant Usage</b>
 * <p>
 * {@link SimpleLock} provides no support for <code>reentrant usage</code>, which means that
 * if current thread acquires the lock successfully, then any invocation of {@link #tryLock()} or
 * {@link #lock()} will fail.
 * <p>
 * <b>No Support for Expiration</b>
 * <p>
 * {@link SimpleLock} does not contains an expiration, implying that if a lock holder does not
 * unlock the lock for some reasons, for example, exceptions are thrown in the scope protected by the
 * lock instance or forget to unlock the lock, then the lock will be hold at server forever such that
 * other threads gains no chances to acquire the lock unless the lock is released at server manually.
 *
 * @author Lam Tong
 * @version 1.0.0
 * @see Lock
 * @since 1.0.0
 */
public final class SimpleLock extends Lock {

    private static final Logger logger = Logger.getLogger(SimpleLock.class.getName());

    private static final ClientProperties properties = ClientProperties.getProperties();

    private static final RequestSender sender = RequestSender.getSender();

    SimpleLock(String key) {
        super(key);
    }

    @Override
    public boolean tryLock() {
        return this.doLock(true);
    }

    @Override
    public boolean lock() {
        return this.doLock(false);
    }

    private boolean doLock(boolean tryLock) {
        if (!this.validateKey()) {
            // If lock key is not available, then returns false immediately.
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, RequestError.EMPTY_LOCK_KEY.getMessage());
            }
            return false;
        }
        if (!this.canLock()) {
            // If this lock instance has been locked successfully before, then returns false immediately.
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, RequestError.LOCKING_ALREADY.getMessage());
            }
            return false;
        }
        LockRequest request = new LockRequest(this.getKey(), properties.getApplication(),
                Thread.currentThread().getName(), Type.SIMPLE_LOCK, tryLock);
        RequestSender requestSender = Objects.requireNonNull(sender.clone());
        Response response;
        if ((response = requestSender.send(request)).isSuccess()) {
            // There are two cases that this code will be executed.
            //     1. lock() is invoked.
            //     2. tryLock() is invoked and lock is acquired.
            this.setCanLock(false);
            this.setSuccess(true);
            this.setCanUnlock(true);
        }
        return response.isSuccess();
    }

    @Override
    public boolean unlock() {
        if (!this.success()) {
            // If this lock instance has not been locked successfully before, then returns immediately.
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, RequestError.LOCKING_FAIL.getMessage());
            }
            return false;
        }
        if (!this.canUnlock()) {
            // If this lock instance has been unlocked, then returns immediately.
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, RequestError.UNLOCKING_ALREADY.getMessage());
            }
            return false;
        }
        UnlockRequest request = new UnlockRequest(this.getKey(), properties.getApplication(),
                Thread.currentThread().getName(), Type.SIMPLE_LOCK);
        RequestSender requestSender = Objects.requireNonNull(sender.clone());
        Response response;
        if ((response = requestSender.send(request)).isSuccess()) {
            // Generally, unlock() always returns true.
            this.setCanUnlock(false);
        }
        return response.isSuccess();
    }

}
