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

import io.github.easylock.common.type.Type;

import java.io.Serializable;

/**
 * {@link UnlockRequest}, sending to a server when a client needs to lock the resources, is
 * an implementation of {@link Request}.
 * <p>
 * <b>Implementation of Identity</b>
 * <p>
 * At present, <code>identity</code> of {@link UnlockRequest} are figured out from the <code>hashcode</code>
 * of a string, which are concat from {@link #key}, {@link #threadName} and string 'unlock'.
 *
 * @author Lam Tong
 * @version 1.0.0
 * @see Request
 * @see LockRequest
 * @since 1.0.0
 */
public final class UnlockRequest extends Request implements Serializable {

    private static final long serialVersionUID = -3469608488768650631L;

    public UnlockRequest(String key, String applicationName,
                         String applicationThreadName, Type type) {
        super(key, applicationName, applicationThreadName, type);
    }

    @Override
    public int getIdentity() {
        return (this.getKey() + this.getThreadName() + "unlock").hashCode();
    }

}
