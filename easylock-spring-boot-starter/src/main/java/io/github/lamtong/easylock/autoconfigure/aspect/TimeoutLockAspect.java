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

import io.github.lamtong.easylock.autoconfigure.annotation.ByTimeoutLock;
import io.github.lamtong.easylock.client.lock.LockFactory;
import io.github.lamtong.easylock.client.lock.TimeoutLock;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link TimeoutLockAspect} processes methods annotated by {@link ByTimeoutLock} using <b>AOP</b> of
 * <code>Spring</code>.
 *
 * @author Lam Tong
 * @version 1.1.2
 * @see ByTimeoutLock
 * @since 1.1.0
 */
@Aspect
@Component
public class TimeoutLockAspect {

    private static final Logger logger = Logger.getLogger(TimeoutLockAspect.class.getName());

    private static final LockFactory factory = new LockFactory();

    @Pointcut(value = "@annotation(io.github.lamtong.easylock.autoconfigure.annotation.ByTimeoutLock)")
    public void timeoutLockAspect() {
        // A point cut.
    }

    @Around(value = "timeoutLockAspect()")
    public Object aroundAdvice(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        ByTimeoutLock annotation = signature.getMethod().getAnnotation(ByTimeoutLock.class);
        String key = annotation.key();
        boolean tryLock = annotation.tryLock();
        boolean skipIfFalse = annotation.skipIfFalse();
        long time = annotation.time();
        TimeUnit unit = annotation.unit();
        if (key.length() == 0) {
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, "Lock key should not be empty, method invocation fails.");
            }
            return null;
        }
        Object ans = null;
        TimeoutLock lock = factory.forTimeoutLock(() -> key);
        if (tryLock) {
            if (lock.tryLock(time, unit)) {
                ans = this.resolveWithLock(lock, point);
            } else {
                if (!skipIfFalse) {
                    ans = this.resolveWithoutLock(point);
                }
            }
        } else {
            if (lock.lock(time, unit)) {
                ans = this.resolveWithLock(lock, point);
            }
        }
        return ans;
    }

    @SuppressWarnings(value = {"Duplicates"})
    private Object resolveWithLock(TimeoutLock lock, ProceedingJoinPoint point) {
        Object ans = null;
        try {
            ans = point.proceed();
        } catch (Throwable e) {
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, e.getMessage());
            }
        } finally {
            if (!lock.unlock() && logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "Unlocking fails, maybe client disconnect from lock server or expiration.");
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

}
