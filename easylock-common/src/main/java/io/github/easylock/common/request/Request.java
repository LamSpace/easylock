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

package io.github.easylock.common.request;

import io.github.easylock.common.response.Response;
import io.github.easylock.common.type.Type;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * Basic definition for both {@link LockRequest} and {@link UnlockRequest}, inheriting
 * from {@link Request}, which includes some basic properties for {@link LockRequest} and
 * {@link UnlockRequest}. <b>Any</b> implementations of {@link Request} may add extra
 * properties to extend the function of {@link Request}. For example, a {@link LockRequest} for
 * <code>TimeoutLock</code> may contains an expiration time and an unit of {@link TimeUnit}.
 * <p>
 * <b>Usage of Identity</b>
 * <p>
 * {@link Request} contains an abstract method named {@link Request#getIdentity()}, which retrieves
 * an identity, which is an unique mark for both {@link LockRequest} and {@link UnlockRequest} and
 * will be passed to corresponding response when a request is resolved at server, allowing request
 * sender, namely client, to check that if corresponding response arrives or not via verifying the
 * identity of both {@link Request} and {@link Response} after sending requests.
 * <p>
 * <b>Caution of Identity</b>
 * <p>
 * Theoretically, each identity from various {@link Request} must differ from each other, which means
 * <ul>
 *     <li>Different threads' {@link Request}s from different clients should differ from each other.</li>
 *     <li>Even for the same thread in a client, {@link LockRequest}'s identity should differ from that
 *     of {@link UnlockRequest}.</li>
 * </ul>
 * <p>
 * <b>Implementation of Identity</b>
 * <p>
 * At present, implementations of identities for both {@link LockRequest} and {@link UnlockRequest} are
 * figured out by {@link #hashCode()}, which may incur <b>Hash Collision</b> in a slim chance, which
 * recommends improvements in the future.
 * <p>
 * <b>Aware of <code>Serializable</code></b>
 * <p>
 * Since that both {@link LockRequest} and {@link UnlockRequest} will be transferred to server via network,
 * {@link Request} are recommended to implement interface {@link Serializable}, and subclasses of
 * {@link Request} are also recommended implementing {@link Serializable} too.
 *
 * @author Lam Tong
 * @version 1.0.0
 * @see LockRequest
 * @see UnlockRequest
 * @since 1.0.0
 */
public abstract class Request implements Serializable {

    private static final long serialVersionUID = -5146190588857856260L;

    private final String key;

    private final String applicationName;

    private final String threadName;

    private final Type type;

    protected Request(String key, String applicationName, String threadName, Type type) {
        this.key = key;
        this.applicationName = applicationName;
        this.threadName = threadName;
        this.type = type;
    }

    public final String getKey() {
        return this.key;
    }

    public final String getApplicationName() {
        return this.applicationName;
    }

    public final String getThreadName() {
        return this.threadName;
    }

    public final Type getType() {
        return this.type;
    }

    public abstract int getIdentity();

    @Override
    public String toString() {
        return "Request{" +
                "key='" + this.key + '\'' +
                ", applicationName='" + this.applicationName + '\'' +
                ", threadName='" + this.threadName + '\'' +
                ", type=" + this.type +
                '}';
    }

}
