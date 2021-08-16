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

import io.github.lamtong.easylock.client.lock.SimpleLock;

import java.lang.annotation.*;

/**
 * {@link BySimpleLock} annotates methods, providing functionality of {@link SimpleLock}
 * by locking and unlocking automatically.
 *
 * @author Lam Tong
 * @version 1.0.0
 * @see SimpleLock
 * @since 1.0.0
 */
@Documented
@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface BySimpleLock {

    /**
     * Lock key for {@link SimpleLock}, must not be empty.
     *
     * @return lock key.
     */
    String key();

    /**
     * If <code>true</code>, then {@link SimpleLock#tryLock()} will be invoked
     * to acquire a lock; otherwise, {@link SimpleLock#lock()} will be invoked.
     *
     * @return true if and only if {@link SimpleLock#tryLock()} is invoked; otherwise, returns false.
     */
    boolean tryLock() default false;

    /**
     * If <code>true</code>, then annotated method will be ignored and not be executed
     * if and only if {@link SimpleLock#tryLock()} returns false; otherwise, that method
     * will also be invoked though {@link SimpleLock#tryLock()} returns false.
     *
     * @return true to ignore the annotated method if and only if {@link SimpleLock#tryLock()} returns false.
     */
    boolean skipIfFalse() default true;

}
