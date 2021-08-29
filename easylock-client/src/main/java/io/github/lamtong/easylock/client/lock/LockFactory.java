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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link LockFactory} tries to generate an instance of specified lock by
 * <ol>
 *     <li>{@link #forLock(int, String)} with specified {@code LockType}, or</li>
 *     <li>{@link #forLock(Class, String)} with specified lock class object.</li>
 * </ol>
 * <p>
 * Besides, {@link LockFactory} provides explicit methods to generate specified locks.
 * For example, {@link #forSimpleLock(String)} retrieves an instance of type {@link SimpleLock}.
 *
 * @author Lam Tong
 * @version 1.2.0
 * @see Lock
 * @see SimpleLock
 * @see TimeoutLock
 * @since 1.0.0
 */
public final class LockFactory {

    private static final Logger logger = Logger.getLogger(LockFactory.class.getName());

    /**
     * Retrieves a lock instance with {@code type} and {@code key}. It can be seen that parameter
     * {@code type} is of type {@code int}, defining lock types by an integer. And the appointment
     * can be list as below:
     * <ul>
     *     <li>Integer number 2 represents {@code Timeout Lock};</li>
     *     <li>Integer number 4 represents {@code Reentrant Lock};</li>
     *     <li>Integer number 8 represents {@code Read-Write Lock}, and</li>
     *     <li>Any other numbers represent {@code Simple Lock}, but 1 is strongly recommended.</li>
     * </ul>
     * In order to acquire accesses for shared resources, generated lock resource should be transformed
     * into an instance of a certain explicit sub-class.
     *
     * @param type lock type.
     * @param key  lock key.
     * @return a lock instance.
     */
    public Lock forLock(int type, String key) {
        switch (type) {
            case 2:
                return new TimeoutLock(key);
            case 4:
                return new ReentrantLock(key);
            case 8:
                return new ReadWriteLock(key);
            default:
                return new SimpleLock(key);
        }
    }

    /**
     * Retrieves a lock instance by <code>Reflection</code>. In this way, type of generated instance
     * is the same as that of parameter {@code Class<T> clazz}. For example, if an instance of type
     * {@link SimpleLock} is demanded, then the following idiom should be used as:
     * <pre>
     *     {@code
     *     ...
     *     LockFactory factory = new Factory();
     *     SimpleLock lock = factory.forLock(SimpleLock.class, "...");
     *     ...
     *     }
     * </pre>
     * In this way, the generated lock instance does not need to be converted into an explicit type
     * compulsorily.
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
     * Retrieves an instance of type {@link SimpleLock} with a functional interface {@link Supplier}.
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
     * Retrieves an instance of type {@link TimeoutLock} with a functional interface {@link Supplier}.
     *
     * @param supplier provides a key for an instance of {@link TimeoutLock}.
     * @return an instance of type {@link TimeoutLock}.
     */
    public TimeoutLock forTimeoutLock(Supplier<String> supplier) {
        return new TimeoutLock(supplier.get());
    }

    /**
     * Retrieves an instance of type {@link ReentrantLock}.
     *
     * @param key lock key.
     * @return an instance of type {@link ReentrantLock}.
     */
    public ReentrantLock forReentrantLock(String key) {
        return new ReentrantLock(key);
    }

    /**
     * Retrieves an instance of type {@link ReentrantLock} with a functional interface {@link Supplier}.
     *
     * @param supplier interface to provide a lock key.
     * @return an instance of type {@link ReentrantLock}.
     */
    public ReentrantLock forReentrantLock(Supplier<String> supplier) {
        return new ReentrantLock(supplier.get());
    }

    /**
     * Retrieves an instance of {@link ReadWriteLock}.
     *
     * @param key lock key.
     * @return an instance of type {@link ReadWriteLock}.
     */
    public ReadWriteLock forRWLock(String key) {
        return new ReadWriteLock(key);
    }

    /**
     * Retrieves an instance of {@link ReadWriteLock} with a functional interface {@link Supplier}
     *
     * @param supplier interface to provide a lock key.
     * @return an instance of type {@link ReadWriteLock}.
     */
    public ReadWriteLock forRWLock(Supplier<String> supplier) {
        return new ReadWriteLock(supplier.get());
    }

    /**
     * Retrieves an instance of type {@link io.github.lamtong.easylock.client.lock.ReadWriteLock.ReadLock} directly.
     *
     * @param key lock key.
     * @return an instance of type {@link io.github.lamtong.easylock.client.lock.ReadWriteLock.ReadLock}.
     */
    public ReadWriteLock.ReadLock forReadLock(String key) {
        return new ReadWriteLock(key).readLock();
    }

    /**
     * Retrieves an instance of type {@link io.github.lamtong.easylock.client.lock.ReadWriteLock.ReadLock} with
     * a functional interface {@link Supplier} directly.
     *
     * @param supplier interface to provide a lock key.
     * @return an instance of type {@link io.github.lamtong.easylock.client.lock.ReadWriteLock.ReadLock}.
     */
    public ReadWriteLock.ReadLock forReadLock(Supplier<String> supplier) {
        return new ReadWriteLock.ReadLock(supplier.get());
    }

    /**
     * Retrieves an instance of type {@link io.github.lamtong.easylock.client.lock.ReadWriteLock.WriteLock} directly.
     *
     * @param key lock key.
     * @return an instance of type {@link io.github.lamtong.easylock.client.lock.ReadWriteLock.WriteLock}.
     */
    public ReadWriteLock.WriteLock forWriteLock(String key) {
        return new ReadWriteLock(key).writeLock();
    }

    /**
     * Retrieves an instance of type {@link io.github.lamtong.easylock.client.lock.ReadWriteLock.WriteLock} using
     * a functional interface {@link Supplier}.
     *
     * @param supplier interface to provide a lock key.
     * @return an instance of type {@link io.github.lamtong.easylock.client.lock.ReadWriteLock.WriteLock}.
     */
    public ReadWriteLock.WriteLock forWriteLock(Supplier<String> supplier) {
        return new ReadWriteLock.WriteLock(supplier.get());
    }

}
