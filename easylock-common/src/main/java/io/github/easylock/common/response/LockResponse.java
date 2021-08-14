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

import io.github.easylock.common.request.LockRequest;

import java.io.Serializable;

/**
 * {@link LockResponse}, an implementation of {@link Response}, indicating that current
 * response corresponds with a {@link LockRequest}.
 *
 * @author Lam Tong
 * @version 1.0.0
 * @see Response
 * @see UnlockResponse
 * @since 1.0.0
 */
public final class LockResponse extends Response implements Serializable {

    private static final long serialVersionUID = 7413414013443625364L;

    public LockResponse(String key, int identity, boolean success, String cause) {
        super(key, identity, success, cause);
    }

}
