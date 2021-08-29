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

package io.github.lamtong.easylock.autoconfigure.aspect;

import io.github.lamtong.easylock.autoconfigure.annotation.ByRWLock;
import io.github.lamtong.easylock.client.lock.LockFactory;
import io.github.lamtong.easylock.client.lock.ReadWriteLock;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link ReadWriteLockAspect} resolves methods annotated by {@link ByRWLock} via <code>AOP</code>.
 *
 * @author Lam Tong
 * @version 1.2.0
 * @see ByRWLock
 * @since 1.2.0
 */
@Aspect
@Component
public class ReadWriteLockAspect {

    private static final Logger logger = Logger.getLogger(ReadWriteLockAspect.class.getName());

    private static final LockFactory factory = new LockFactory();

    @Pointcut(value = "@annotation(io.github.lamtong.easylock.autoconfigure.annotation.ByRWLock)")
    public void readWriteLockAspect() {
        // Empty body for a point cut.
    }

    @Around(value = "readWriteLockAspect()")
    public Object aroundAdvice(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        ByRWLock annotation = signature.getMethod().getAnnotation(ByRWLock.class);
        String key = annotation.key();
        boolean readLock = annotation.readLock();
        boolean tryLock = annotation.tryLock();
        boolean skipIfFalse = annotation.skipIfFalse();
        if (key.length() == 0) {
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, "Lock key should not be empty, method invocation fails.");
            }
            return null;
        }
        ReadWriteLock readWriteLock = factory.forRWLock(() -> key);
        Object ans = null;
        if (readLock) {
            ReadWriteLock.ReadLock lock = readWriteLock.readLock();
            if (tryLock) {
                if (lock.tryLock()) {
                    ans = this.resolveWithReadLock(lock, point);
                } else {
                    if (!skipIfFalse) {
                        ans = this.resolveWithoutLock(point);
                    }
                }
            } else {
                if (lock.lock()) {
                    ans = this.resolveWithReadLock(lock, point);
                }
            }
        } else {
            ReadWriteLock.WriteLock lock = readWriteLock.writeLock();
            if (tryLock) {
                if (lock.tryLock()) {
                    ans = this.resolveWithWriteLock(lock, point);
                } else {
                    if (!skipIfFalse) {
                        ans = this.resolveWithoutLock(point);
                    }
                }
            } else {
                if (lock.lock()) {
                    ans = this.resolveWithWriteLock(lock, point);
                }
            }
        }
        return ans;
    }

    @SuppressWarnings(value = {"Duplicates"})
    private Object resolveWithReadLock(ReadWriteLock.ReadLock lock, ProceedingJoinPoint point) {
        Object ans = null;
        try {
            ans = point.proceed();
        } catch (Throwable e) {
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, e.getMessage());
            }
        } finally {
            if (!lock.unlock() && logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "Unlocking for ReadLock fails, maybe client disconnect from server.");
            }
        }
        return ans;
    }

    @SuppressWarnings(value = {"Duplicates"})
    private Object resolveWithoutLock(ProceedingJoinPoint point) {
        Object ans = null;
        try {
            ans = point.proceed();
        } catch (Throwable e) {
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, e.getMessage());
            }
        }
        return ans;
    }

    @SuppressWarnings(value = {"Duplicates"})
    private Object resolveWithWriteLock(ReadWriteLock.WriteLock lock, ProceedingJoinPoint point) {
        Object ans = null;
        try {
            ans = point.proceed();
        } catch (Throwable e) {
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, e.getMessage());
            }
        } finally {
            if (!lock.unlock() && logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "Unlocking for WriteLock fails, maybe client disconnect from server.");
            }
        }
        return ans;
    }

}
