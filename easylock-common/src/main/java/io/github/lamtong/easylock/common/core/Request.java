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

package io.github.lamtong.easylock.common.core;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Basic definition for {@code Lock Request} and {@code Unlock Request}.
 * <p>
 * <b>Usage of <code>Identity</code></b>
 * <p>
 * {@link Request} includes a method called {@link Request#getIdentity()} to retrieve an identity, which is
 * a unique mark for both {@code Lock Request} and {@code Unlock Request}, and will be passed to corresponding
 * response when a {@link Request} instance is resolved at server, allowing the request sender, namely client,
 * to check that whether corresponding response arrives or not via verifying the identity of both <code>Lock
 * Request</code> and <code>Unlock Request</code> after sending requests.
 * <p>
 * <b>Caution of <code>Identity</code></b>
 * <p>
 * Theoretically, each identity from various {@link Request} must differ from each other, which means
 * <ul>
 *     <li>Different threads' {@link Request}s from different clients should differ from each other;</li>
 *     <li>Even for the same thread in a client, {@code Lock Request}'s identity should differ from that of
 *     <code>Unlock Request</code>.</li>
 * </ul>
 * <p>
 * <b>Implementation of <code>Identity</code></b>
 * <p>
 * At present, identities for both <code>Lock Request</code> and <code>Unlock Request</code> are derived from
 * {@link String#hashCode()} of a concat <code>String</code>, which may incur <b>Hash Collision</b> in a slim
 * chance and needs improvements in the future.
 * <p>
 * <b>Aware of <code>Serializable</code> interface</b>
 * <p>
 * Since that both <code>Lock Request</code> and <code>Unlock Request</code> will be transferred to server via network,
 * implementation of {@link Serializable} is strongly recommended.
 * <p>
 * <b>Work Flow</b>
 * <p>
 * Work flow of {@link Request} resolved at server can be listed as below:
 * <ol>
 *     <li>Checks {@code Lock Type} of current {@link Request} instance;</li>
 *     <li>Checks whether current {@link Request} instance is a <code>Lock Request</code> or an <code>Unlock
 *     Request</code> via {@link Request#lockRequest}. If current {@link Request} is a <code>Lock Request</code>,
 *     then go to step 3; otherwise, resolves that <code>Unlock Request</code>;</li>
 *     <li>Checks that current instance is a <code>try-lock</code> {@link Request} or not via {@link #tryLock}
 *     and resolves that <code>Lock Request</code>.</li>
 * </ol>
 *
 * @author Lam Tong
 * @version 1.2.0
 * @see Serializable
 * @since 1.0.0
 */
public final class Request implements Serializable {

    private static final long serialVersionUID = -4046412243351543543L;

    private final String key;

    private final String application;

    private final String thread;

    /**
     * Lock type defines by integer. It is an appointment that
     * <ul>
     *     <li>'1' represents a simple lock;</li>
     *     <li>'2' represents a timeout lock;</li>
     *     <li>'4' represents a reentrant lock, and</li>
     *     <li>'8' represents a read-write lock.</li>
     * </ul>
     */
    private final int type;

    private final boolean lockRequest;

    private final boolean tryLock;

    private final long time;

    private final TimeUnit timeUnit;

    private final boolean readLock;

    /**
     * Constructor for {@code Simple Lock} and {@code Reentrant Lock}, whether a lock request
     * which only support lock operation or an unlock request.
     *
     * @param key
     * @param application
     * @param thread
     * @param type
     * @param lockRequest
     */
    @SuppressWarnings(value = {"JavaDoc"})
    public Request(String key, String application, String thread,
                   int type, boolean lockRequest) {
        this(key, application, thread, type, lockRequest, false);
    }

    /**
     * Constructor for {@code Simple Lock} and {@code Reentrant Lock}, whether a lock request
     * or an unlock request.
     *
     * @param key
     * @param application
     * @param thread
     * @param type
     * @param lockRequest
     * @param tryLock
     */
    @SuppressWarnings(value = {"JavaDoc"})
    public Request(String key, String application, String thread,
                   int type, boolean lockRequest, boolean tryLock) {
        this(key, application, thread, type, lockRequest, tryLock, false);
    }

    /**
     * Typical constructor for {@code Timeout Lock}, whether a lock request or an unlock request.
     *
     * @param key
     * @param application
     * @param thread
     * @param type
     * @param lockRequest
     * @param tryLock
     * @param time
     * @param timeUnit
     */
    @SuppressWarnings(value = {"JavaDoc"})
    public Request(String key, String application, String thread,
                   int type, boolean lockRequest, boolean tryLock,
                   long time, TimeUnit timeUnit) {
        this(key, application, thread, type, lockRequest, tryLock, time, timeUnit, false);
    }

    /**
     * Typical constructor for {@code Read-Write Lock}, whether a lock request or an unlock
     * request.
     *
     * @param key
     * @param application
     * @param thread
     * @param type
     * @param lockRequest
     * @param tryLock
     * @param readLock
     */
    @SuppressWarnings(value = {"JavaDoc"})
    public Request(String key, String application, String thread,
                   int type, boolean lockRequest, boolean tryLock,
                   boolean readLock) {
        this(key, application, thread, type, lockRequest, tryLock, 0, null, readLock);
    }

    /**
     * Constructor for four kinds of locks, which are {@code Simple Lock}, {@code Timeout Lock},
     * {@code Reentrant Lock} and {@code Read-Write Lock} with all parameters, whether a lock
     * request or an unlock request.
     *
     * @param key         lock key
     * @param application application name
     * @param thread      thread name
     * @param type        lock type, defined by an integer
     * @param lockRequest if current request is a lock request
     * @param tryLock     if current lock request acquires a lock resource with <code>try-lock</code>.
     * @param time        time to expire
     * @param timeUnit    time unit for expiration
     * @param readLock    if current request is a read lock or not
     */
    public Request(String key, String application, String thread,
                   int type, boolean lockRequest, boolean tryLock,
                   long time, TimeUnit timeUnit, boolean readLock) {
        this.key = key;
        this.application = application;
        this.thread = thread;
        this.type = type;
        this.lockRequest = lockRequest;
        this.tryLock = tryLock;
        this.time = time;
        this.timeUnit = timeUnit;
        this.readLock = readLock;
    }

    public String getKey() {
        return key;
    }

    public String getApplication() {
        return application;
    }

    public String getThread() {
        return thread;
    }

    public int getType() {
        return type;
    }

    public boolean isLockRequest() {
        return lockRequest;
    }

    public boolean isTryLock() {
        return tryLock;
    }

    public long getTime() {
        return time;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public boolean isReadLock() {
        return readLock;
    }

    public int getIdentity() {
        // TODO: 2021/8/28 仔细确认 identity 与哪些元素相关.
        return (this.key + this.thread + this.lockName() + this.requestName()).hashCode();
    }

    private String lockName() {
        switch (this.type) {
            case 1:
                return "SimpleLock";
            case 2:
                return "TimeoutLock";
            case 4:
                return "ReentrantLock";
            default:
                return "ReadWriteLock";
        }
    }

    private String requestName() {
        return this.lockRequest ? "Lock" : "Unlock";
    }

    @Override
    public String toString() {
        return "Request{" +
                "key='" + key + '\'' +
                ", application='" + application + '\'' +
                ", thread='" + thread + '\'' +
                ", type=" + this.lockName() +
                ", lock=" + lockRequest +
                ", tryLock=" + tryLock +
                ", time=" + time +
                ", timeUnit=" + timeUnit +
                ", readLock=" + readLock +
                '}';
    }

}
