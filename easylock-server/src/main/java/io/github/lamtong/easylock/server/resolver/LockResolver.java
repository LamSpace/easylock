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

package io.github.lamtong.easylock.server.resolver;


import io.github.lamtong.easylock.common.core.Request;
import io.github.lamtong.easylock.common.core.Response;

/**
 * {@link LockResolver} defines three operations to resolve {@code LockRequest} or {@code UnlockRequest}.
 * Any implementation of {@link LockResolver} should override these methods to resolve requests for
 * a certain type of lock.
 *
 * @author Lam Tong
 * @version 1.0.0
 * @since 1.0.0
 */
public interface LockResolver {

    Response resolveTryLock(Request lockRequest);

    Response resolveLock(Request lockRequest);

    Response resolveUnlock(Request unlockRequest);

}
