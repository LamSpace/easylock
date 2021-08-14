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

package io.github.easylock.server.resolver;

import io.github.easylock.common.request.LockRequest;
import io.github.easylock.common.request.Request;
import io.github.easylock.common.request.UnlockRequest;
import io.github.easylock.common.response.LockResponse;
import io.github.easylock.common.response.Response;
import io.github.easylock.common.response.UnlockResponse;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link AbstractLockResolver} implements interface {@link Resolver} and {@link LockResolver}
 * at the same time to provide a template to resolve lock or unlock requests. Any implementations
 * of {@link AbstractLockResolver} should not and also can not override {@link #resolve(Request)}.
 * <p>
 * As an abstract class, {@link AbstractLockResolver} contains some fields to support the process
 * of {@link LockRequest} and {@link UnlockRequest}, such as {@link #lockHolder}, {@link #lockMonitor}
 * {@link #permissions} and {@link #requests}.
 * <p>
 * <b>Procedure of Locking</b>
 * <p>
 * Locking procedure is resolved by imitating via {@link #lockHolder} of type {@link ConcurrentHashMap},
 * which is the core of <code>lock resolver</code>. Generally, if one thread of any application succeeds
 * in acquiring the lock, other threads' {@link LockRequest} may fail unless that lock is released. When
 * resolving {@link LockRequest} with <code>lock</code> rather than <code>try-lock</code>, <b>BlockingQueue</b>
 * is used to resolve the blocking {@link LockRequest}s, waking up other waiting threads to acquire a
 * permission for locking.
 *
 * @author Lam Tong
 * @version 1.0.0
 * @see Resolver
 * @see LockResolver
 * @since 1.0.0
 */
public abstract class AbstractLockResolver implements Resolver, LockResolver {

    protected static final String SUCCEED = "", LOCKED_ALREADY = "Lock has been locked already.",
            LOCK_EXPIRED = "Lock has expired already.";

    protected final ConcurrentHashMap<String, LockRequest> lockHolder = new ConcurrentHashMap<>();

    protected final Object lockMonitor = new Object();

    protected final ConcurrentHashMap<String, BlockingQueue<Object>> requests = new ConcurrentHashMap<>(),
            permissions = new ConcurrentHashMap<>();

    @Override
    public final Response resolve(Request request) {
        if (request instanceof LockRequest) {
            LockRequest lockRequest = (LockRequest) request;
            if (lockRequest.isTryLock()) {
                return this.resolveTryLock(lockRequest);
            }
            return this.resolveLock(lockRequest);
        }
        return this.resolveUnlock(((UnlockRequest) request));
    }

    @Override
    public abstract LockResponse resolveTryLock(LockRequest lockRequest);

    @Override
    public abstract LockResponse resolveLock(LockRequest lockRequest);

    @Override
    public abstract UnlockResponse resolveUnlock(UnlockRequest unlockRequest);

}
