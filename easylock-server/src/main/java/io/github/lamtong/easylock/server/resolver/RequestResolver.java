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
 * {@link RequestResolver} resolves lock or unlock requests for kinds of locks. Generally,
 * {@link RequestResolver} contains instances of {@link AbstractLockResolver} to resolve
 * requests via {@link Request#type}. For example, {@link RequestResolver} contains a static
 * field of type {@link SimpleLockResolver} to resolve requests for {@code SIMPLE_LOCK}.
 * <p>
 * Note that instance of {@link RequestResolver} is often used to resolve received requests.
 * <ol>
 *     <li>Dispatches requests to corresponding <code>lock resolver</code> by invoking
 *     {@link AbstractLockResolver#resolve(Request)}.</li>
 *     <li>Resolves requests in a specified <code>lock resolver</code> by checking that if
 *     current {@link Request} is a {@code LockRequest} or {@code UnlockRequest} and if current
 *     {@code LockRequest} is a <code>try-lock</code> or not if current {@link Request} is a
 *     {@code LockRequest}.</li>
 * </ol>
 * <p>
 * Be aware that {@link AbstractLockResolver#resolve(Request)} defines a procedure to resolve
 * a certain type of lock's requests. And <b>any</b> implementations of {@link AbstractLockResolver}
 * should not override this method.
 *
 * @author Lam Tong
 * @version 1.2.0
 * @see Resolver
 * @see AbstractLockResolver
 * @since 1.0.0
 */
public final class RequestResolver implements Resolver {

    private static final SimpleLockResolver simpleLock = SimpleLockResolver.getResolver();

    private static final TimeoutLockResolver timeoutLock = TimeoutLockResolver.getResolver();

    private static final ReentrantLockResolver reentrantLock = ReentrantLockResolver.getResolver();

    private static final ReadWriteLockResolver readWriteLock = ReadWriteLockResolver.getResolver();

    @Override
    public Response resolve(Request request) {
        switch (request.getType()) {
            case 2:
                return timeoutLock.resolve(request);
            case 4:
                return reentrantLock.resolve(request);
            case 8:
                return readWriteLock.resolve(request);
            default:
                return simpleLock.resolve(request);
        }
    }

}
