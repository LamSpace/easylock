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

import io.github.lamtong.easylock.common.core.Request;

/**
 * Constant definitions of {@link Request} errors.
 *
 * @author Lam Tong
 * @version 1.3.1
 * @see Request
 * @since 1.2.0
 */
final class RequestError {

    static final String EMPTY_LOCK_KEY = "Lock key should not be null or empty, reset lock key.";

    static final String LOCKING_ALREADY = "Locking succeeds already, lock cancels.";

    static final String LOCKING_FAIL = "Locking fails before, unlock cancels.";

    static final String UNLOCKING_ALREADY = "Unlocking succeeds already, unlock cancels.";

    private RequestError() {
    }

}