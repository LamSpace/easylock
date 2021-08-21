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

import io.github.lamtong.easylock.common.type.LockType;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link LockFactory} tries to generate an instance of specified lock by
 * <ol>
 *     <li>{@link #forLock(LockType, String)} with specified {@link LockType}, or</li>
 *     <li>{@link #forLock(Class, String)} with specified lock class object.</li>
 * </ol>
 * <p>
 * Besides, {@link LockFactory} provides explicit methods to generate specified locks.
 * For example, {@link #forSimpleLock(String)} retrieves an instance of type {@link SimpleLock}.
 *
 * @author Lam Tong
 * @version 1.1.0
 * @see Lock
 * @see SimpleLock
 * @see TimeoutLock
 * @since 1.0.0
 */
public final class LockFactory {

    private static final Logger logger = Logger.getLogger(LockFactory.class.getName());

    /**
     * Retrieves a lock instance by {@link LockType}.
     *
     * @param type lock type.
     * @param key  lock key.
     * @return a lock instance.
     */
    public Lock forLock(LockType type, String key) {
        // In the future, <code>if</code> block should be replaced with <code>switch</code>
        if (type == LockType.SIMPLE_LOCK) {
            return new SimpleLock(key);
        }
        return null;
    }

    /**
     * Retrieves a lock instance by <code>Reflection</code>.
     *
     * @param clazz specified lock class object.
     * @param key   lock key.
     * @param <T>   object inheriting class {@link Lock}.
     * @return a lock instance.
     */
    public <T> T forLock(Class<T> clazz, String key) {
        T t = null;
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor(String.class);
            t = constructor.newInstance(key);
        } catch (NoSuchMethodException | InstantiationException |
                IllegalAccessException | InvocationTargetException e) {
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, e.getMessage());
            }
        }
        return t;
    }

    /**
     * Retrieves an instance of type {@link SimpleLock}.
     *
     * @param key lock key.
     * @return an instance of type {@link SimpleLock}.
     */
    public SimpleLock forSimpleLock(String key) {
        return new SimpleLock(key);
    }

    /**
     * Retrieves an instance of type {@link SimpleLock} using a functional interface {@link Supplier}.
     *
     * @param supplier offers a key for an instance of {@link SimpleLock}.
     * @return an instance of type {@link SimpleLock}.
     */
    public SimpleLock forSimpleLock(Supplier<String> supplier) {
        return new SimpleLock(supplier.get());
    }

    /**
     * Retrieves an instance of type {@link TimeoutLock}.
     *
     * @param key lock key.
     * @return an instance of type {@link TimeoutLock}.
     */
    public TimeoutLock forTimeoutLock(String key) {
        return new TimeoutLock(key);
    }

    /**
     * Retrieves an instance of type {@link TimeoutLock} using a functional interface {@link Supplier}.
     *
     * @param supplier provides a key for an instance of {@link TimeoutLock}.
     * @return an instance of type {@link TimeoutLock}.
     */
    public TimeoutLock forTimeoutLock(Supplier<String> supplier) {
        return new TimeoutLock(supplier.get());
    }

}
