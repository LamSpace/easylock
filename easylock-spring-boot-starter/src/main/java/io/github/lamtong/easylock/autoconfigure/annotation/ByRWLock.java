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

import io.github.lamtong.easylock.client.lock.ReadWriteLock;

import java.lang.annotation.*;

/**
 * {@link ByRWLock} annotates on methods, providing functionality of {@link ReadWriteLock} by
 * locking and unlocking automatically.
 *
 * @author Lam Tong
 * @version 1.2.0
 * @see ReadWriteLock
 * @since 1.2.0
 */
@Documented
@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface ByRWLock {

    /**
     * Lock key for {@link ReadWriteLock}, must not be empty.
     *
     * @return lock key.
     */
    String key();

    /**
     * If {@code true}, then {@link ReadWriteLock.ReadLock} will be used for locking and unlocking; otherwise,
     * {@link ReadWriteLock.WriteLock} is adopted.
     *
     * @return true for {@link ReadWriteLock.ReadLock}; otherwise, returns false.
     */
    boolean readLock() default false;

    /**
     * If {@code true}, {@code try-lock} will be invoked to acquire a lock resource; otherwise, {@code lock} will
     * be invoked.
     *
     * @return true if and only if {@code try-lock} is invoked; otherwise, returns false.
     */
    boolean tryLock() default false;

    /**
     * If {@code true}, annotated method will not be invoked if and only if {@code try-lock} returns false.
     *
     * @return true to ignore annotated method if {@code try-lock} returns false.
     */
    boolean skipIfFalse() default true;

}
