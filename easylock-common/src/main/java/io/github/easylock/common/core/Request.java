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

package io.github.easylock.common.core;

import io.github.easylock.common.type.LockType;
import io.github.easylock.common.type.RequestType;

import java.io.Serializable;

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
 *     <li>Checks {@link LockType} of current {@link Request} instance;</li>
 *     <li>Checks whether current {@link Request} instance is a <code>Lock Request</code> or an <code>Unlock
 *     Request</code> via {@link Request#requestType}. If current {@link Request} is a <code>Lock Request</code>,
 *     then go to step 3; otherwise, resolves that <code>Unlock Request</code>;</li>
 *     <li>Checks that current instance is a <code>try-lock</code> {@link Request} or not via {@link #tryLock}
 *     and resolves that <code>Lock Request</code>.</li>
 * </ol>
 *
 * @author Lam Tong
 * @version 1.0.0
 * @see LockType
 * @see RequestType
 * @since 1.0.0
 */
public final class Request implements Serializable {

    private static final long serialVersionUID = -4046412243351543543L;

    private final String key;

    private final String application;

    private final String thread;

    private final LockType lockType;

    private final RequestType requestType;

    private final boolean tryLock;

    public Request(String key, String application, String thread,
                   LockType lockType, RequestType requestType) {
        this(key, application, thread, lockType, requestType, false);
    }

    public Request(String key, String application, String thread,
                   LockType lockType, RequestType requestType,
                   boolean tryLock) {
        this.key = key;
        this.application = application;
        this.thread = thread;
        this.lockType = lockType;
        this.requestType = requestType;
        this.tryLock = tryLock;
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

    public LockType getLockType() {
        return lockType;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public boolean isTryLock() {
        return tryLock;
    }

    public int getIdentity() {
        return (this.key + this.thread + this.requestType.name()).hashCode();
    }

    @Override
    public String toString() {
        return "Request{" +
                "key='" + key + '\'' +
                ", application='" + application + '\'' +
                ", thread='" + thread + '\'' +
                ", lockType=" + lockType +
                ", requestType=" + requestType +
                ", tryLock=" + tryLock +
                '}';
    }

}
