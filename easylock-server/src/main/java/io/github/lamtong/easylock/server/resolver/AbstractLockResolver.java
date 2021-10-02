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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link AbstractLockResolver} implements interface {@link LockResolver}, providing a template to
 * resolve lock or unlock requests. Any implementations of {@link AbstractLockResolver} should not
 * and also can not override {@link #resolve(Request.RequestProto)}.
 * <p>
 * As an abstract class, {@link AbstractLockResolver} contains some fields to support the process
 * of {@code LockRequest} and {@code UnlockRequest}, such as {@link #lockHolder}, {@link #permissions}
 * and {@link #requests}.
 * <p>
 * <b>Procedure of Locking</b>
 * <p>
 * Locking procedure is resolved by imitating via {@link #lockHolder} of type {@link ConcurrentHashMap},
 * which is the core of <code>lock resolver</code>. Generally, if one thread of any application succeeds
 * in acquiring the lock, other threads' {@code LockRequest} may fail unless that lock is released. When
 * resolving {@code LockRequest} with <code>lock</code> rather than <code>try-lock</code>, <b>BlockingQueue</b>
 * is used to resolve the blocking {@code LockRequest}s, waking up other waiting threads to acquire a
 * permission for locking.
 *
 * @author Lam Tong
 * @version 1.3.1
 * @see LockResolver
 * @since 1.0.0
 */
public abstract class AbstractLockResolver implements LockResolver {

    protected static final String SUCCEED = "";

    protected static final String LOCKED_ALREADY = "Lock has been locked already.";

    protected static final String LOCK_EXPIRED = "Lock has expired already.";

    protected final ConcurrentHashMap<String, Request.RequestProto> lockHolder = new ConcurrentHashMap<>();

    protected final ConcurrentHashMap<String, BlockingQueue<Object>> requests = new ConcurrentHashMap<>();

    protected final ConcurrentHashMap<String, BlockingQueue<Object>> permissions = new ConcurrentHashMap<>();

    @Override
    public final Response.ResponseProto resolve(Request.RequestProto request) {
        if (request.getLockRequest()) {
            if (request.getTryLock()) {
                return this.resolveTryLock(request);
            }
            return this.resolveLock(request);
        }
        return this.resolveUnlock(request);
    }

    /**
     * Retrieves a message when acquiring a lock.
     *
     * @param request lock request.
     * @return a message when acquiring a lock resource.
     */
    public abstract String acquireLock(Request.RequestProto request);

    /**
     * Retrieves a message when releasing a lock.
     *
     * @param request unlock request.
     * @return a message when releasing an
     */
    public abstract String releaseLock(Request.RequestProto request);

    /**
     * Checks that whether lock resource has been acquires at least once.
     *
     * @param request lock request
     * @return true if and only if lock resource has been acquires at least once.
     */
    public abstract boolean isLocked(Request.RequestProto request);

}
