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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link SimpleLockResolver} extends {@link AbstractLockResolver} and overrides those abstract methods
 * to fulfill the process for {@code LockRequest} and {@code UnlockRequest} due to that
 * {@link AbstractLockResolver#resolve(Request.RequestProto)} has already define a template to resolve requests.
 *
 * @author Lam Tong
 * @version 1.3.1
 * @see AbstractLockResolver
 * @since 1.0.0
 */
public final class SimpleLockResolver extends AbstractLockResolver {

    private static final Logger logger = Logger.getLogger(SimpleLockResolver.class.getName());

    /**
     * Singleton of {@link SimpleLockResolver}.
     */
    private static final SimpleLockResolver resolver = new SimpleLockResolver();

    private SimpleLockResolver() {
    }

    public static SimpleLockResolver getResolver() {
        return resolver;
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public Response.ResponseProto resolveTryLock(Request.RequestProto lockRequest) {
        String key = lockRequest.getKey();
        Request.RequestProto request = this.lockHolder.putIfAbsent(key, lockRequest);
        if (request == null) {
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, acquireLock(lockRequest));
            }
            return Response.ResponseProto.newBuilder()
                    .setKey(key)
                    .setIdentity(lockRequest.getIdentity())
                    .setSuccess(true)
                    .setCause(SUCCEED)
                    .setLockResponse(true)
                    .build();
        }
        return Response.ResponseProto.newBuilder()
                .setKey(key)
                .setIdentity(lockRequest.getIdentity())
                .setSuccess(false)
                .setCause(LOCKED_ALREADY)
                .setLockResponse(true)
                .build();
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public Response.ResponseProto resolveLock(Request.RequestProto lockRequest) {
        String key = lockRequest.getKey();
        Request.RequestProto request = this.lockHolder.putIfAbsent(key, lockRequest);
        if (request == null) {
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, acquireLock(lockRequest));
            }
            return Response.ResponseProto.newBuilder()
                    .setKey(key)
                    .setIdentity(lockRequest.getIdentity())
                    .setSuccess(true)
                    .setCause(SUCCEED)
                    .setLockResponse(true)
                    .build();
        }
        this.requests.putIfAbsent(key, new LinkedBlockingQueue<>());
        this.permissions.putIfAbsent(key, new SynchronousQueue<>());
        try {
            this.requests.get(key).put(new Object());
            this.permissions.get(key).take();
            this.lockHolder.put(key, lockRequest);
        } catch (InterruptedException e) {
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, e.getMessage());
            }
            Thread.currentThread().interrupt();
        }
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, acquireLock(lockRequest));
        }
        return Response.ResponseProto.newBuilder()
                .setKey(key)
                .setIdentity(lockRequest.getIdentity())
                .setSuccess(true)
                .setCause(SUCCEED)
                .setLockResponse(true)
                .build();
    }

    @Override
    @SuppressWarnings(value = {"Duplicates"})
    public Response.ResponseProto resolveUnlock(Request.RequestProto unlockRequest) {
        String key = unlockRequest.getKey();
        this.lockHolder.remove(key);
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, releaseLock(unlockRequest));
        }
        try {
            if (this.requests.containsKey(key)) {
                if (!this.requests.get(key).isEmpty()) {
                    // If there exists more than one request to be resolved.
                    this.requests.get(key).take();
                    this.permissions.get(key).put(new Object());
                } else {
                    // Current unlock request is the last request to be resolved for that type of lock.
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
        return Response.ResponseProto.newBuilder()
                .setKey(key)
                .setIdentity(unlockRequest.getIdentity())
                .setSuccess(true)
                .setCause(SUCCEED)
                .setLockResponse(false)
                .build();
    }

    @Override
    public String acquireLock(Request.RequestProto request) {
        return String.format("[%s] - [%s] acquires SimpleLock successfully.",
                request.getApplication(), request.getThread());
    }

    @Override
    public String releaseLock(Request.RequestProto request) {
        return String.format("[%s] - [%s] releases SimpleLock successfully.",
                request.getApplication(), request.getThread());
    }

    @Override
    public boolean isLocked(Request.RequestProto request) {
        return false;
    }

}
