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

import io.github.lamtong.easylock.common.type.ResponseType;

import java.io.Serializable;

/**
 * Definition for both <code>Lock Response</code> and <code>Unlock Response</code>, with some basic
 * properties. For example, field {@link #success} and {@link #cause} tell whether the request resolved
 * successfully or not and the reason if fails.
 * <p>
 * <b>Usage of <code>Identity</code></b>
 * <p>
 * Field {@link #identity} of {@link Response} is passed from a <code>Lock Request</code> or
 * an <code>Unlock Request</code> and it should be, since that {@link #identity} is an essential
 * mark to let the client known that corresponding response arrives asynchronously, which will be
 * stored in a pool.
 * <p>
 * <b>Aware of <code>Serializable</code> interface</b>
 * <p>
 * Since that both <code>Lock Response</code> and <code>Unlock Response</code> will be transferred
 * from server to clients via network, implementation of {@link Serializable} counts.
 *
 * @author Lam Tong
 * @version 1.0.0
 * @see ResponseType
 * @since 1.0.0
 */
public final class Response implements Serializable {

    private static final long serialVersionUID = 881044494721596185L;

    private final String key;

    private final int identity;

    private final boolean success;

    private final String cause;

    private final ResponseType responseType;

    public Response(String key, int identity, boolean success,
                    String cause, ResponseType responseType) {
        this.key = key;
        this.identity = identity;
        this.success = success;
        this.cause = cause;
        this.responseType = responseType;
    }

    public String getKey() {
        return key;
    }

    public int getIdentity() {
        return identity;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCause() {
        return cause;
    }

    public ResponseType getResponseType() {
        return responseType;
    }

    @Override
    public String toString() {
        return "Response{" +
                "key='" + key + '\'' +
                ", identity=" + identity +
                ", success=" + success +
                ", cause='" + cause + '\'' +
                ", responseType=" + responseType +
                '}';
    }

}
