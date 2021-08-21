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

package io.github.lamtong.easylock.client.lock;

import io.github.lamtong.easylock.client.constant.RequestError;
import io.github.lamtong.easylock.client.property.ClientProperties;
import io.github.lamtong.easylock.client.sender.RequestSender;
import io.github.lamtong.easylock.common.core.Request;
import io.github.lamtong.easylock.common.core.Response;
import io.github.lamtong.easylock.common.type.LockType;
import io.github.lamtong.easylock.common.type.RequestType;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of {@link Lock}, supporting expiration usage.
 * <p>
 * {@link TimeoutLock} indicates that when locking succeeds by {@link #tryLock(long, TimeUnit)} or
 * {@link #lock(long, TimeUnit)}, an expiration message will be added into an instance of Type
 * {@link DelayQueue} at server. When an expiration message is taken from that {@link DelayQueue}
 * instance, corresponding lock will be removed. Here, an expiration implies the maximum duration for
 * access control of shared resources protected by this lock resource for clients. Once this lock
 * resource expires, shared resources will no longer be protected since this lock is released at server.
 * Any other threads may acquire the lock resource and {@link #unlock()} fails with the same reason but
 * does not throw an exception.
 * <p>
 * It is recommended that the expiration should be greater that duration to access the shared resources
 * slightly but not too great.
 * <p>
 * <b>No Support for Reentrant Usage</b>
 * <p>
 * {@link TimeoutLock} does not support for reentrant usage, indicating that this lock resource can only
 * be acquired once.
 * <p>
 *
 * @author Lam Tong
 * @version 1.1.0
 * @see Lock
 * @since 1.1.0
 */
@SuppressWarnings(value = {"Duplicates"})
public class TimeoutLock extends Lock {

    private static final Logger logger = Logger.getLogger(TimeoutLock.class.getName());

    private static final ClientProperties properties = ClientProperties.getProperties();

    private static final RequestSender sender = RequestSender.getSender();

    TimeoutLock(String key) {
        super(key);
    }

    /**
     * Tries to acquire the lock and returns true if and only if the lock resource is available at
     * server with specified expiration. If the lock resource is not available then this method
     * returns false immediately. Namely, this method does only send a lock request to server once
     * and acquires a lock response immediately, representing whether locking operation succeeds or not.
     *
     * @param time     expiration time
     * @param timeUnit unit for expiration time
     * @return true if and only if the lock resource if available; otherwise, returns false.
     */
    public boolean tryLock(long time, TimeUnit timeUnit) {
        return doLock(true, time, timeUnit);
    }

    /**
     * Acquires the lock resource with blocking attempt
     * <p>
     * If the lock resource is not available then this lock request will block until the lock resource
     * is available and always returns true, which means this method never fails.
     *
     * @param time     expiration time
     * @param timeUnit unit for expiration time
     * @return true always
     */
    public boolean lock(long time, TimeUnit timeUnit) {
        return doLock(false, time, timeUnit);
    }

    public boolean doLock(boolean tryLock, long time, TimeUnit timeUnit) {
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
        Request request = new Request(this.getKey(), properties.getApplication(),
                Thread.currentThread().getName(), LockType.TIMEOUT_LOCK, RequestType.LOCK_REQUEST,
                tryLock, time, timeUnit);
        Response response = sender.send(request);
        if (response.isSuccess()) {
//             There are two cases that this code will be executed.
//                 1. lock() is invoked.
//                 2. tryLock() is invoked and lock is acquired.
            this.setCanLock(false);
            this.setCanUnlock(true);
            this.setSuccess(true);
        }
        return response.isSuccess();
    }

    /**
     * Overloaded by {@link #tryLock(long, TimeUnit)} to provide an extra functionality of expiration.
     *
     * @return false cause this method will not be invoked by clients.
     */
    @Override
    protected boolean tryLock() {
        return false;
    }

    /**
     * Overloaded by {@link #lock(long, TimeUnit)} to provide an extra functionality of expiration.
     *
     * @return false cause this method will not be invoked by clients.
     */
    @Override
    protected boolean lock() {
        return false;
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
        Request request = new Request(this.getKey(), properties.getApplication(),
                Thread.currentThread().getName(), LockType.TIMEOUT_LOCK, RequestType.UNLOCK_REQUEST);
        Response response = sender.send(request);
        if (response.isSuccess()) {
            // Generally, unlock() always returns true.
            this.setCanUnlock(false);
        }
        return response.isSuccess();
    }

}
