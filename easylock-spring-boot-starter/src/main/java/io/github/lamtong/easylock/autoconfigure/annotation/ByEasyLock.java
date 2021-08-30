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

import io.github.lamtong.easylock.client.lock.Lock;
import io.github.lamtong.easylock.client.lock.ReadWriteLock;
import io.github.lamtong.easylock.client.lock.SimpleLock;
import io.github.lamtong.easylock.client.lock.TimeoutLock;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * {@link ByEasyLock} is a composite annotation, which provides functionalities of {@link SimpleLock},
 * {@link TimeoutLock} and {@link ReadWriteLock} for locking and unlocking automatically.
 *
 * @author Lam Tong
 * @version 1.2.1
 * @see SimpleLock
 * @see BySimpleLock
 * @see TimeoutLock
 * @see ByTimeoutLock
 * @see ReadWriteLock
 * @see ByRWLock
 * @since 1.2.1
 */
@Documented
@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface ByEasyLock {

    /**
     * An explicit class which inherits {@link Lock}.
     *
     * @return an explicit class.
     */
    Class<? extends Lock> clazz();

    /**
     * Lock key for {@code EasyLock}, must not be empty.
     *
     * @return lock key.
     */
    String key();

    /**
     * Whether {@code tryLock()} of {@code tryLock(long, TimeUnit)} is invoked or not.
     *
     * @return true if and only if {@code tryLock()} or {@code tryLock(long, TimeUnit)} is invoked; otherwise,
     * returns false.
     */
    boolean tryLock() default false;

    /**
     * Whether annotated method is ignored or not when {@code tryLock()} or {@code tryLock(long, TimeUnit)} returns
     * false.
     *
     * @return true if and only if annotated method is ignored when {@code tryLock()} or {@code tryLock(long, TimeUnit)}
     * returns false; otherwise, returns false.
     */
    boolean skipIfFalse() default true;

    /**
     * Time to expire for {@link TimeoutLock}
     *
     * @return expiration time.
     */
    long time() default 1L;

    /**
     * Time unit for expiration of {@link TimeoutLock}.
     *
     * @return expiration unit.
     */
    TimeUnit unit() default TimeUnit.SECONDS;

    /**
     * Whether {@link io.github.lamtong.easylock.client.lock.ReadWriteLock.ReadLock} is required or not for
     * {@link ReadWriteLock}.
     *
     * @return true if and only if {@link io.github.lamtong.easylock.client.lock.ReadWriteLock.ReadLock} if required;
     * otherwise, returns false.
     */
    boolean readLock() default false;

}
