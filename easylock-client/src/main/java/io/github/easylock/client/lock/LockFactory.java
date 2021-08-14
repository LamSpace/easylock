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

package io.github.easylock.client.lock;

import io.github.easylock.common.core.Request;
import io.github.easylock.common.type.LockType;

/**
 * {@link LockFactory} generates an explicit {@link Lock} implementation with specified {@link LockType}.
 * And {@link Request#key} should also be specified.
 * <p>
 * It is strongly recommended that generated lock should be transformed to specified type since
 * {@link #getLock(LockType, String)} only returns an instance with type {@link Lock}, and methods of
 * that instance can not be accessed publicly. Implementations of {@link Lock} overrides implicit
 * methods and modifiers also. For example,
 * <pre>
 *     {@code
 * SimpleLock lock = ((SimpleLock) LockFactory.getLock(Type.SimpleLock, "aaa"));
 * ...
 * lock.lock();
 * ...
 * lock.unlock();
 * ...
 *     }
 * </pre>
 *
 * @author Lam Tong
 * @version 1.0.0
 * @see Lock
 * @see SimpleLock
 * @since 1.0.0
 */
public final class LockFactory {

    // TODO: 2021/8/14 优化LockFactory 
    public static Lock getLock(LockType lockType, String key) {
        return new SimpleLock(key);
    }

}
