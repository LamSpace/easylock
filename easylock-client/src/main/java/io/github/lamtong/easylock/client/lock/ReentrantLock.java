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
import io.github.lamtong.easylock.client.identity.DefaultIdentityGenerator;
import io.github.lamtong.easylock.client.identity.IdentityGenerator;
import io.github.lamtong.easylock.common.core.Request;
import io.github.lamtong.easylock.common.core.Response;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of class {@link Lock}.
 * <p>
 * <b>Support for Reentrant Usage</b>
 * <p>
 * {@link ReentrantLock} indicates that this kind of lock resource support reentrant usage, implying that a
 * lock instance can acquire that lock again after locking succeeds via {@link #tryLock()} or {@link #lock()}.
 * {@link ReentrantLock} instance includes a counter to record counts of successful locking operations. When
 * unlocking, {@link #unlock()} should be invoked as the same time as successful locking operations by
 * {@link #tryLock()} if returns true and {@link #lock()}.
 * <p>
 * It is strongly recommended that {@link #tryLock()} should be invoked for the first time to acquire the
 * lock resource. And if returns true, {@link #lock()} can be invoked directly with no competition from other
 * threads.
 * <p>
 * <b>No Support for Expiration</b>
 * <p>
 * Note that {@link ReentrantLock} still does not support for expiration. Thus care must be taken when locking
 * succeeds and the lock resource should be unlocked completely and timely, avoiding the lock is hold forever
 * at server.
 *
 * @author Lam Tong
 * @version 1.3.2
 * @see Lock
 * @since 1.1.0
 */
@SuppressWarnings(value = {"Duplicates"})
public final class ReentrantLock extends Lock {

    private static final Logger logger = Logger.getLogger(ReentrantLock.class.getName());

    private static final RequestSender sender = RequestSender.getSender();

    private static final ClientProperties properties = ClientProperties.getProperties();

    private static final IdentityGenerator generator = DefaultIdentityGenerator.getInstance();

    private int count;

    ReentrantLock(String key) {
        super(key);
    }

    @Override
    public boolean tryLock() {
        return doLock(true);
    }

    @Override
    public boolean lock() {
        return doLock(false);
    }

    private boolean doLock(boolean tryLock) {
        if (!this.validateKey()) {
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, RequestError.EMPTY_LOCK_KEY);
            }
            return false;
        }
        Request.RequestProto request = Request.RequestProto.newBuilder()
                .setKey(this.getKey())
                .setApplication(properties.getApplication())
                .setThread(Thread.currentThread().getName())
                .setType(4)
                .setLockRequest(true)
                .setTryLock(tryLock)
                .setIdentity(generator.generate())
                .build();
        Response.ResponseProto response = sender.send(request);
        if (response.getSuccess()) {
            this.setCanUnlock(true);
            this.setSuccess(true);
            this.count++;
        }
        return response.getSuccess();
    }

    @Override
    public boolean unlock() {
        if (!this.success()) {
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, RequestError.LOCKING_FAIL);
            }
            return false;
        }
        if (!this.canUnlock()) {
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, RequestError.UNLOCKING_ALREADY);
            }
            return false;
        }
        Request.RequestProto request = Request.RequestProto.newBuilder()
                .setKey(this.getKey())
                .setApplication(properties.getApplication())
                .setThread(Thread.currentThread().getName())
                .setType(4)
                .setLockRequest(false)
                .setIdentity(generator.generate())
                .build();
        Response.ResponseProto response = sender.send(request);
        if (response.getSuccess()) {
            // If unlock succeeds, decrement lock count and set canUnlock to false until lock
            // count back to zero.
            this.count--;
            if (this.count == 0) {
                this.setCanUnlock(false);
            }
        }
        return response.getSuccess();
    }

}
