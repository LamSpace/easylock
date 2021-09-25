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

import io.github.lamtong.easylock.client.connection.ClientProperties;
import io.github.lamtong.easylock.client.connection.RequestSender;
import io.github.lamtong.easylock.common.core.Request;
import io.github.lamtong.easylock.common.core.Response;

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
 *
 * @author Lam Tong
 * @version 1.3.1
 * @see Lock
 * @since 1.1.0
 */
@SuppressWarnings(value = {"Duplicates"})
public final class TimeoutLock extends Lock {

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

    private boolean doLock(boolean tryLock, long time, TimeUnit timeUnit) {
        if (!this.validateKey()) {
            // If lock key is not available, then returns false immediately.
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, RequestError.EMPTY_LOCK_KEY);
            }
            return false;
        }
        if (!this.canLock()) {
            // If this lock instance has been locked successfully before, then returns false immediately.
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, RequestError.LOCKING_ALREADY);
            }
            return false;
        }
        Request.RequestProto request = Request.RequestProto.newBuilder()
                .setKey(this.getKey())
                .setApplication(properties.getApplication())
                .setThread(Thread.currentThread().getName())
                .setType(2)
                .setLockRequest(true)
                .setTryLock(tryLock)
                .setTime(timeUnit.toMillis(time))
                .setIdentity((this.getKey() + Thread.currentThread().getName() + "TimeoutLock" + "Lock").hashCode())
                .build();
        Response.ResponseProto response = sender.send(request);
        if (response.getSuccess()) {
//             There are two cases that this code will be executed.
//                 1. lock() is invoked.
//                 2. tryLock() is invoked and lock is acquired.
            this.setCanLock(false);
            this.setCanUnlock(true);
            this.setSuccess(true);
        }
        return response.getSuccess();
    }

    /**
     * Tries to acquire the lock and returns true if and only if the lock resource is available
     * with default expiration.
     *
     * @return true if and only if the lock resource if available; otherwise, returns false.
     */
    @Override
    public boolean tryLock() {
        return tryLock(1, TimeUnit.SECONDS);
    }

    /**
     * Acquires the lock resource with default expiration and never fails.
     *
     * @return true always.
     */
    @Override
    public boolean lock() {
        return lock(1, TimeUnit.SECONDS);
    }

    @Override
    public boolean unlock() {
        if (!this.success()) {
            // If this lock instance has not been locked successfully before, then returns immediately.
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, RequestError.LOCKING_FAIL);
            }
            return false;
        }
        if (!this.canUnlock()) {
            // If this lock instance has been unlocked, then returns immediately.
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, RequestError.UNLOCKING_ALREADY);
            }
            return false;
        }
        Request.RequestProto request = Request.RequestProto.newBuilder()
                .setKey(this.getKey())
                .setApplication(properties.getApplication())
                .setThread(Thread.currentThread().getName())
                .setType(2)
                .setLockRequest(false)
                .setIdentity((this.getKey() + Thread.currentThread().getName() + "SimpleLock" + "Unlock").hashCode())
                .build();
        Response.ResponseProto response = sender.send(request);
        if (response.getSuccess()) {
            // Generally, unlock() always returns true.
            this.setCanUnlock(false);
        }
        return response.getSuccess();
    }

}
