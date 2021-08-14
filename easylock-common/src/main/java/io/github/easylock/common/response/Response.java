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

package io.github.easylock.common.response;

import java.io.Serializable;

/**
 * Abstract {@link Response} definition for both {@link LockResponse} and {@link UnlockResponse},
 * including some basic properties, such as {@link #key}, {@link #identity}, {@link #success} and
 * {@link #cause}. When a client sends a request successfully, corresponding response transferred
 * from server will be checked by verifying {@link #key} and {@link #identity}. Besides, {@link #success}
 * and {@link #cause} tell whether request processed successfully or not at server and the reason
 * if fail.
 * <p>
 * <b>Usage of Identity</b>
 * <p>
 * Field {@link #identity} of {@link Response} is passed from a lock or unlock request, and it should
 * be since field {@link #identity} is an essential mark to let the client known that corresponding
 * response arrives asynchronously, storing in a pool.
 * <p>
 * <b>Aware of {@link Serializable}</b>
 * <p>
 * Be aware that implementations of {@link Response} will be transferred via network, thus {@link Response}
 * must implement interface {@link Serializable} and <b>any</b> implementations of {@link Response} must
 * implement interface {@link Serializable}.
 *
 * @author Lam Tong
 * @version 1.0.0
 * @see LockResponse
 * @see UnlockResponse
 * @since 1.0.0
 */
public abstract class Response implements Serializable {

    private static final long serialVersionUID = -5198394762789027813L;

    private final String key;

    private final int identity;

    private final boolean success;

    private final String cause;

    protected Response(String key, int identity, boolean success, String cause) {
        this.key = key;
        this.identity = identity;
        this.success = success;
        this.cause = cause;
    }

    public final String getKey() {
        return this.key;
    }

    public final int getIdentity() {
        return this.identity;
    }

    public final boolean isSuccess() {
        return this.success;
    }

    public final String getCause() {
        return this.cause;
    }

    @Override
    public String toString() {
        return "Response{" +
                "key='" + this.key + '\'' +
                ", identity=" + this.identity +
                ", success=" + this.success +
                ", cause='" + this.cause + '\'' +
                '}';
    }

}
