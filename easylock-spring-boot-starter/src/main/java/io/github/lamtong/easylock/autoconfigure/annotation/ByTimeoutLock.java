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

package io.github.lamtong.easylock.autoconfigure.annotation;

import io.github.lamtong.easylock.client.lock.TimeoutLock;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * {@link ByTimeoutLock} annotates on methods, providing functionality of {@link TimeoutLock}
 * by locking and unlocking automatically.
 *
 * @author Lam Tong
 * @version 1.1.0
 * @see TimeoutLock
 * @since 1.1.0
 */
@Documented
@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface ByTimeoutLock {

    /**
     * Lock key for {@link TimeoutLock}, must not be empty.
     *
     * @return lock key.
     */
    String key();

    /**
     * Expiration for {@link TimeoutLock}.
     *
     * @return expiration time.
     */
    long time() default 1L;

    /**
     * Expiration time unit for {@link TimeoutLock}.
     *
     * @return Time unit.
     */
    TimeUnit unit() default TimeUnit.SECONDS;

    /**
     * If {@code true}, the {@link TimeoutLock#tryLock(long, TimeUnit)} will be invoked to
     * acquire a lock resource; otherwise, {@link TimeoutLock#lock(long, TimeUnit)} will invoked.
     *
     * @return true if and only if {@link TimeoutLock#tryLock(long, TimeUnit)} if invoked; otherwise, returns false.
     */
    boolean tryLock() default false;

    /**
     * If {@code true}, then annotated method will be ignored and not be executed if and only if
     * {@link TimeoutLock#tryLock(long, TimeUnit)} returns false; otherwise, that method will also be
     * invoked though {@link TimeoutLock#tryLock(long, TimeUnit)} returns false.
     *
     * @return true if and only if annotated method is ignored when {@link TimeoutLock#tryLock(long, TimeUnit)}
     * returns false.
     */
    boolean skipIfFalse() default true;

}
