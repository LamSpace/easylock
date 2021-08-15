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

package io.github.easylock.autoconfigure.aspect;

import io.github.easylock.autoconfigure.annotation.BySimpleLock;
import io.github.easylock.client.lock.LockFactory;
import io.github.easylock.client.lock.SimpleLock;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link SimpleLockAspect} processes methods annotated by {@link BySimpleLock} via <b>AOP</b>
 * of <code>Spring</code>.
 *
 * @author Lam Tong
 * @version 1.0.0
 * @see BySimpleLock
 * @since 1.0.0
 */
@Aspect
@Component
public class SimpleLockAspect {

    private static final Logger logger = Logger.getLogger(SimpleLockAspect.class.getName());

    private static final LockFactory factory = new LockFactory();

    @Pointcut(value = "@annotation(io.github.easylock.autoconfigure.annotation.BySimpleLock)")
    public void simpleLockAspect() {
        // A point cut.
    }

    @Around(value = "simpleLockAspect()")
    public Object aroundAdvice(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        BySimpleLock bySimpleLock = signature.getMethod().getAnnotation(BySimpleLock.class);
        String key = bySimpleLock.key();
        boolean tryLock = bySimpleLock.tryLock();
        boolean skipIfFalse = bySimpleLock.skipIfFalse();
        if (key.length() == 0) {
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, "Lock key should not be empty, method invocation fails.");
            }
            return null;
        }
        SimpleLock lock = factory.forSimpleLock(key);
        Object ans = null;
        if (tryLock) {
            if (lock.tryLock()) {
                ans = this.resolveWithLock(lock, point);
            } else {
                if (!skipIfFalse) {
                    ans = this.resolveWithoutLock(point);
                }
            }
        } else {
            if (lock.lock()) {
                ans = this.resolveWithLock(lock, point);
            }
        }
        return ans;
    }

    private Object resolveWithLock(SimpleLock lock, ProceedingJoinPoint point) {
        Object ans = null;
        try {
            ans = point.proceed();
        } catch (Throwable e) {
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, e.getMessage());
            }
        } finally {
            if (!lock.unlock() && logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "Unlocking fails, maybe client disconnect from lock server.");
            }
        }
        return ans;
    }

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
