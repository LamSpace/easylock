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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link ReadWriteLock} provides an inclusive {@link ReadLock} and an exclisive {@link WriteLock}.
 * <p>
 * <b>Implementation Consideration</b>
 * <p>
 * {@link ReadLock} of {@link ReadWriteLock} is inclusive, which implies that {@link ReadLock} allows
 * shared resources can be accessed by other threads without modifying shared resources which {@link WriteLock}
 * is exclusive. It can be inferred that only one thread can acquire a {@link WriteLock} resource at a time. Other
 * threads will never gain a chance to acquire a {@link WriteLock} resource until that lock resource is released
 * in time.
 * <p>
 * <b>Support for Downgrade</b>
 * <p>
 * <b>Downgrade</b> of {@link ReadWriteLock} indicates that a {@link WriteLock} can downgrade to a
 * {@link ReadLock}. For example, if one thread acquires a {@link WriteLock} resource successfully and has
 * not released that resource yet, then
 * <ol>
 *     <li>Any other threads, who are going to acquire a {@link WriteLock} will fail.</li>
 *     <li>Any other threads, who are going to acquire a {@link ReadLock} will fail too.</li>
 * </ol>
 * But, if that thread tries to acquire a {@link ReadLock} resource with {@link WriteLock} resource holding on,
 * it will succeed. And then that thread holds a {@link WriteLock} resource and a {@link ReadLock} resource
 * simultaneously. If that thread releases the {@link WriteLock} resource, then only {@link ReadLock} resource
 * is hold. In other words, the {@link WriteLock} downgrades to {@link ReadLock}. Here the idiom can be used as
 * <pre>
 *     {@code
 *     ReadWriteLock lock = new ReadWriteLock("...");
 *     ReadWriteLock.ReadLock readLock = lock.readLock();
 *     ReadWriteLock.WriteLock writeLock = lock.writeLock();
 *
 *     ...
 *     writeLock.lock();
 *     ...
 *     readLock.lock();
 *     ...
 *     writeLock.unlock();  // WriteLock downgrades to ReadLock.
 *     ...
 *     readLock.unlock()l
 *     }
 * </pre>
 * <p>
 * <b>No Support for Reentrant Usage</b>
 * <p>
 * At present, {@link ReadWriteLock} does not support for {@code Reentrant Usage}, indicating that if one thread
 * acquires a {@link ReadLock} resource or a {@link WriteLock} resource successfully, then invocation of
 * {@code lock()} or {@code tryLock()} fails immediately.
 *
 * @author Lam Tong
 * @version 1.3.1
 * @see Lock
 * @since 1.2.0
 */
public final class ReadWriteLock extends Lock {

    private static final Logger logger = Logger.getLogger(ReadWriteLock.class.getName());

    private static final RequestSender sender = RequestSender.getSender();

    private static final ClientProperties properties = ClientProperties.getProperties();

    ReadWriteLock(String key) {
        super(key);
    }

    /**
     * Retrieves a {@link ReadLock} resource.
     *
     * @return a {@link ReadLock} resource.
     */
    public ReadLock readLock() {
        return new ReadLock(this.getKey());
    }

    /**
     * Retrieves a {@link WriteLock} resource.
     *
     * @return a {@link WriteLock} resource.
     */
    public WriteLock writeLock() {
        return new WriteLock(this.getKey());
    }

    @Override
    protected boolean tryLock() {
        return false;
    }

    @Override
    protected boolean lock() {
        return false;
    }

    @Override
    protected boolean unlock() {
        return false;
    }

    /**
     * {@link ReadLock} of {@link ReadWriteLock}, which is inclusive. It can be inferred that shared resources
     * can be accessed by threads which are holding on a {@link ReadLock} resource respectively without modifying
     * the shared resources.
     *
     * @author Lam Tong
     * @version 1.2.0
     * @see Lock
     * @since 1.2.0
     */
    @SuppressWarnings(value = {"Duplicates"})
    public static final class ReadLock extends Lock {

        ReadLock(String key) {
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
            if (!this.canLock()) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.log(Level.INFO, RequestError.LOCKING_ALREADY);
                }
                return false;
            }
            Request.RequestProto request = Request.RequestProto.newBuilder()
                    .setKey(this.getKey())
                    .setApplication(properties.getApplication())
                    .setThread(Thread.currentThread().getName())
                    .setType(8)
                    .setLockRequest(true)
                    .setTryLock(tryLock)
                    .setReadLock(true)
                    .setIdentity((this.getKey() + Thread.currentThread().getName() + "ReadWriteLock" + "Lock").hashCode())
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
                    .setType(8)
                    .setLockRequest(false)
                    .setTryLock(false)
                    .setReadLock(true)
                    .setIdentity((this.getKey() + Thread.currentThread().getName() + "ReadWriteLock" + "Unlock").hashCode())
                    .build();
            Response.ResponseProto response = sender.send(request);
            if (response.getSuccess()) {
//                 Generally, unlock() always returns true.
                this.setCanUnlock(false);
            }
            return response.getSuccess();
        }

    }

    /**
     * Exclusive {@link WriteLock} of {@link ReadWriteLock}, which indicates that if one thread holds on
     * an instance {@link WriteLock} for shared resources, then any other accesses from other threads will
     * be blocked until that thread releases the lock resource.
     *
     * @author Lam Tong
     * @version 1.2.0
     * @see Lock
     * @since 1.2.0
     */
    @SuppressWarnings(value = {"Duplicates"})
    public static final class WriteLock extends Lock {

        WriteLock(String key) {
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
            if (!this.canLock()) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.log(Level.INFO, RequestError.LOCKING_ALREADY);
                }
                return false;
            }
            Request.RequestProto request = Request.RequestProto.newBuilder()
                    .setKey(this.getKey())
                    .setApplication(properties.getApplication())
                    .setThread(Thread.currentThread().getName())
                    .setType(8)
                    .setLockRequest(true)
                    .setTryLock(tryLock)
                    .setReadLock(false)
                    .setIdentity((this.getKey() + Thread.currentThread().getName() + "ReadWriteLock" + "Lock").hashCode())
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
                    .setType(8)
                    .setLockRequest(false)
                    .setTryLock(false)
                    .setReadLock(false)
                    .setIdentity((this.getKey() + Thread.currentThread().getName() + "ReadWriteLock" + "Unlock").hashCode())
                    .build();
            Response.ResponseProto response = sender.send(request);
            if (response.getSuccess()) {
                // Generally, unlock() always returns true.
                this.setCanUnlock(false);
            }
            return response.getSuccess();
        }

    }

}
