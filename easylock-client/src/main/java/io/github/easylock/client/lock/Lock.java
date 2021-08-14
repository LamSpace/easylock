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

import io.github.easylock.common.type.Type;

/**
 * {@link Lock} implementations provides more extensive lock or unlock operations in a distributed
 * application, where <b>synchronized</b> methods or other {@link java.util.concurrent.locks.Lock}
 * implementations can not. They allow more flexible structuring, having quite different properties.
 * <p>
 * Generally, a lock is a tool for controlling access to a shared resource by multiple threads.
 * Nevertheless, resources can not be shared by threads from multiple sub-applications in a distributed
 * application. Commonly, a lock provides exclusive access to a shared resource: only one thread
 * at a time can acquire the lock and all accesses to the shared resource require that the lock be
 * acquired first. However, some locks may allow concurrent accesses to a shared resource, such as
 * <code>read lock</code> of a <code>ReadWriteLock</code>.
 * <p>
 * With this increased flexibility comes additional responsibility. In most cases, the following
 * idiom should be used:
 *
 * <pre>
 *     {@code
 *     Lock lock = ...
 *     try {
 *         ...
 *     } finally {
 *         lock.unlock();
 *     }}
 * </pre>
 * <p>
 * Here, locking and unlocking are encouraged to appear in the same scope. When locking and unlocking
 * occur in different scopes, care must be taken to ensure that all code, which will be executed while
 * the lock is held, is protected by <code>try-finally</code> or <code>try-catch-finally</code> to
 * ensure that the lock is released when necessary.
 * <p>
 * {@link Lock} implementations may provide additional functionality by offering a non-blocking attempt
 * to acquire a lock via {@link Lock#tryLock()}, or add an expiration time for a lock if locking
 * succeeds and reentrant usage.
 * <p>
 * A {@link Lock} class can also provide behavior and semantics that is quite different from that of the
 * implicit monitor lock, such as reentrant usage.
 * <p>
 * Note that {@link Lock} instances are just normal objects and can themselves be used as the target in
 * a <code>synchronized</code> statement. Acquiring the monitor lock of a {@link Lock} instance has no
 * specified relationship with invoking any of the {@link #lock()} methods of that instance. Thus, it is
 * recommended that to avoid confusion you never use <code>Lock</code> instances in this way strongly.
 * <p>
 * In a distributed application, it is <b>mandatory</b> to specify the lock name (or lock key {@link #key})
 * to distinguish distributed locks with the same lock type {@link Type} for locking and unlocking.
 * <p>
 * Implementations of {@link Lock} never throw an exception when locking or unlocking fails. But it is
 * still strongly recommended that locking and unlocking should be used in a <b>try-catch</b> or
 * <b>try-finally</b> scope, avoiding lock is held forever in lock server incurred by exceptions
 * thrown from code access the resource protected by {@code Lock} instances. Even the lock has an expiration
 * time, which means lock will be released after a certain period after locking succeeds, it is recommended
 * to do so.
 * <p>
 * Besides, {@link Lock} provides no interruption, which implies that locking can not be canceled.
 * <p>
 * Be aware that connection from client to lock server may be down, thus locking and unlocking requests
 * can not be sent to lock server. That is, if connection has not been established, locking or unlocking
 * will fail.
 *
 * @author Lam Tong
 * @version 1.0.0
 * @see SimpleLock
 * @since 1.0.0
 */
public abstract class Lock {

    /**
     * Lock key
     */
    private final String key;

    private boolean success = false;

    private boolean canLock = true;

    private boolean canUnlock = false;

    Lock(String key) {
        this.key = key;
    }

    /**
     * Acquires the lock if and only if it is available at the time when invoked.
     * <p>
     * Acquires the lock if it is available and returns with the value <code>true</code> immediately.
     * If the lock is not available then this method will return immediately with the value
     * <code>false</code>. Namely, {@link #tryLock()} only sends a lock request to lock server once
     * and acquires a lock response immediately, representing whether locking succeeds or not.
     * <p>
     * A typical usage idiom for this method would be:
     * <pre>
     *     {@code
     *     Lock lock = ...;
     *     if (lock.tryLock()) {
     *         try {
     *             // manipulate protected state
     *         } finally {
     *             lock.unlock();
     *         }
     *     } else {
     *         // perform alternative actions
     *     }}
     * </pre>
     * <p>
     * This usage ensures that the lock is unlocked if it was acquired, and does not try to unlock
     * if the lock was not acquired.
     * <p>
     * <b>Implementation Considerations</b>
     * <p>
     * Only an initialized lock instance, not support for reentrant lock, can invoke {@link #tryLock()}.
     * Once a lock instance succeeds in locking, lock request should be intercepted and returns immediately.
     *
     * @return {@code true} if the lock was acquired and {@code false} otherwise.
     */
    protected abstract boolean tryLock();

    /**
     * Acquires a lock with blocking attempt.
     * <p>
     * If the lock is not available then current thread will wait until the lock is available at server,
     * which implies the lock has been acquired. Namely, {@link #lock()} never fails, <b>except</b>
     * for that the server is down, in other words, connection from client to lock server has not been
     * established.
     * <p>
     * <b>Implementation Consideration</b>
     * <p>
     * Only an initialized lock instance, not support for reentrant lock, can invoke {@link #lock()}.
     * Once a lock instance succeeds in locking, lock request should be intercepted and returns immediately.
     * <p>
     * <b>No support for interruption</b>
     *
     * @return {@code true} if lock was acquired; otherwise, returns {@code false}.
     */
    protected abstract boolean lock();

    /**
     * Releases the lock on condition that the lock has been acquired successfully before.
     * <p>
     * <b>Implementation Consideration</b>
     * <p>
     * A <code>Lock</code> implementation will usually impose restriction on which the thread can
     * release a lock (typically, only the holder of the lock, which has not been unlocked or unlocked
     * completely before, can invoke {@link #unlock()}). It throws no exceptions as mush as possible.
     *
     * @return {@code true} if and only if unlocking succeeds; otherwise, returns {@code false}.
     */
    protected abstract boolean unlock();

    /**
     * Validates that if the lock <code>key</code> is available or not.
     *
     * @return true if and only if the key is not null and not empty; otherwise, returns false.
     */
    boolean validateKey() {
        return this.key != null && this.key.length() > 0;
    }

    String getKey() {
        return this.key;
    }

    boolean success() {
        return this.success;
    }

    void setSuccess(boolean success) {
        this.success = success;
    }

    boolean canLock() {
        return this.canLock;
    }

    void setCanLock(boolean canLock) {
        this.canLock = canLock;
    }

    boolean canUnlock() {
        return this.canUnlock;
    }

    void setCanUnlock(boolean canUnlock) {
        this.canUnlock = canUnlock;
    }

}
