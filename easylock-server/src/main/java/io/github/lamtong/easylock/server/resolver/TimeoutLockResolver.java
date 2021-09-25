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

import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link TimeoutLockResolver} extends {@link AbstractLockResolver} to resolve {@code Lock Request} and
 * {@code Unlock Request} for {@code TimeoutLock}.
 * <p>
 * <b>Implementation Consideration.</b>
 * <p>
 * To handle requests for {@code TimeoutLock}, an extra thread is recommended to release locks which expired
 * by an instance of type {@link DelayQueue}.
 *
 * @author Lam Tong
 * @version 1.3.1
 * @see AbstractLockResolver
 * @since 1.1.0
 */
public final class TimeoutLockResolver extends AbstractLockResolver {

    private static final Logger logger = Logger.getLogger(TimeoutLockResolver.class.getName());

    private static final TimeoutLockResolver resolver = new TimeoutLockResolver();

    private final DelayQueue<DelayLock> delayLocks = new DelayQueue<>();

    private TimeoutLockResolver() {
        new Thread(() -> {
            for (; ; ) {
                try {
                    DelayLock lock = this.delayLocks.take();
                    Request.RequestProto request = this.lockHolder.get(lock.key);
                    if (request != null && request.getIdentity() == lock.identity) {
                        // If expired lock request has not been removed from lock holder, then removes it.
                        this.lockHolder.remove(lock.key);
                        if (logger.isLoggable(Level.INFO)) {
                            logger.log(Level.INFO, String.format("[%s] removes expired TimeoutLock [%s].",
                                    Thread.currentThread().getName(), request.getKey()));
                        }
                        try {
                            if (this.requests.containsKey(lock.key)) {
                                if (!this.requests.get(lock.key).isEmpty()) {
                                    this.requests.get(lock.key).take();
                                    this.permissions.get(lock.key).put(new Object());
                                } else {
                                    this.requests.remove(lock.key);
                                    this.permissions.remove(lock.key);
                                }
                            }
                        } catch (InterruptedException e) {
                            if (logger.isLoggable(Level.SEVERE)) {
                                logger.log(Level.SEVERE, e.getMessage());
                            }
                            Thread.currentThread().interrupt();
                        }
                    }
                } catch (InterruptedException e) {
                    if (logger.isLoggable(Level.SEVERE)) {
                        logger.log(Level.SEVERE, e.getMessage());
                    }
                    Thread.currentThread().interrupt();
                }
            }
        }, "TimeoutLock-Consumer").start();
    }

    public static TimeoutLockResolver getResolver() {
        return resolver;
    }

    @Override
    @SuppressWarnings(value = {"Duplicates"})
    public Response.ResponseProto resolveTryLock(Request.RequestProto lockRequest) {
        String key = lockRequest.getKey();
        Request.RequestProto request = this.lockHolder.putIfAbsent(key, lockRequest);
        if (request == null) {
            this.delayLocks.put(new DelayLock(key, lockRequest.getIdentity(),
                    lockRequest.getTime()));
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
    @SuppressWarnings(value = {"Duplicates"})
    public Response.ResponseProto resolveLock(Request.RequestProto lockRequest) {
        String key = lockRequest.getKey();
        Request.RequestProto request = this.lockHolder.putIfAbsent(key, lockRequest);
        if (request == null) {
            this.delayLocks.put(new DelayLock(key, lockRequest.getIdentity(),
                    lockRequest.getTime()));
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
            this.delayLocks.put(new DelayLock(key, lockRequest.getIdentity(),
                    lockRequest.getTime()));
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
        Request.RequestProto request = this.lockHolder.get(key);
        if (request != null) {
            // Lock is still hold at server
            if (request.getApplication().equals(unlockRequest.getApplication()) &&
                    request.getThread().equals(unlockRequest.getThread())) {
                // Unlock succeeds within expiration
                this.lockHolder.remove(key);
                if (logger.isLoggable(Level.INFO)) {
                    logger.log(Level.INFO, releaseLock(unlockRequest));
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
                return Response.ResponseProto.newBuilder()
                        .setKey(key)
                        .setIdentity(unlockRequest.getIdentity())
                        .setSuccess(true)
                        .setCause(SUCCEED)
                        .setLockResponse(false)
                        .build();
            } else {
                // Lock expired and now is hold be another thread.
                if (logger.isLoggable(Level.INFO)) {
                    logger.log(Level.INFO, String.format("[%s] - [%s] releases TimeoutLock unsuccessfully, " +
                                    "cause this lock has expired already and is hold by [%s] - [%s].",
                            unlockRequest.getApplication(), unlockRequest.getThread(),
                            request.getApplication(), request.getThread()));
                }
                return Response.ResponseProto.newBuilder()
                        .setKey(key)
                        .setIdentity(unlockRequest.getIdentity())
                        .setSuccess(true)
                        .setCause(LOCK_EXPIRED)
                        .setLockResponse(false)
                        .build();
            }
        } else {
            // Lock is not hold at server, namely expired.
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, String.format("[%s] - [%s] releases TimeoutLock unsuccessfully " +
                        "due to expiration.", unlockRequest.getApplication(), unlockRequest.getThread()));
            }
            return Response.ResponseProto.newBuilder()
                    .setKey(key)
                    .setIdentity(unlockRequest.getIdentity())
                    .setSuccess(true)
                    .setCause(LOCK_EXPIRED)
                    .setLockResponse(false)
                    .build();
        }
    }

    @Override
    public String acquireLock(Request.RequestProto request) {
        return String.format("[%s] - [%s] acquires TimeoutLock successfully.",
                request.getApplication(), request.getThread());
    }

    @Override
    public String releaseLock(Request.RequestProto request) {
        return String.format("[%s] - [%s] releases TimeoutLock successfully.",
                request.getApplication(), request.getThread());
    }

    @Override
    public boolean isLocked(Request.RequestProto request) {
        return false;
    }

    /**
     * Entity definition to record lock's information in {@link DelayQueue}.
     *
     * @author Lam Tong
     * @version 1.3.1
     * @see Delayed
     * @since 1.1.0
     */
    private static class DelayLock implements Delayed {

        private final long time;

        String key;

        long identity;

        public DelayLock(String key, long identity, long time) {
            this.key = key;
            this.identity = identity;
            this.time = System.currentTimeMillis() + (time > 0 ? time : 0);
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return this.time - System.currentTimeMillis();
        }

        @Override
        public int compareTo(Delayed o) {
            DelayLock lock = (DelayLock) o;
            return this.time - lock.time <= 0 ? -1 : 1;
        }

    }

}
