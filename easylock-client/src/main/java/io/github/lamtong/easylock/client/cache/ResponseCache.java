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

package io.github.lamtong.easylock.client.cache;

import io.github.lamtong.easylock.client.property.ClientProperties;
import io.github.lamtong.easylock.common.core.Request;
import io.github.lamtong.easylock.common.core.Response;

import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link ResponseCache} provides a pool to store arrived responses transferred from server. Since that
 * the lock client sends lock or unlock requests asynchronously, then corresponding responses arrive
 * asynchronously, too. Hence {@link ResponseCache} provides a way to let the thread which sends a lock
 * or unlock request to check that whether corresponding response arrives or not.
 * <p>
 * Since that a client may send plenty of lock or unlock requests, thus it is essential to distinguish
 * those requests. Hence, each response contains a field named {@link Response#key}, which is passed from
 * corresponding {@link Request}. {@link Request}-send thread will check the <code>key</code> first to check
 * if corresponding response of that type, namely the <code>key</code>. And if the response with that key
 * arrives, then {@link Response#identity} will be verified for each thread.
 *
 * @author Lam Tong
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ResponseCache {

    private static final Logger logger = Logger.getLogger(ResponseCache.class.getName());

    private static final ResponseCache cache = new ResponseCache();

    /**
     * Cache pool to store received responses for threads.
     */
    private final ConcurrentHashMap<String, BlockingQueue<Response>> cachePool = new ConcurrentHashMap<>();

    private ResponseCache() {
    }

    public static ResponseCache getCache() {
        return cache;
    }

    /**
     * Tries to retrieve the first element from the cache queue with specified lock key,
     * while not remove.
     *
     * @param key specified key for lock.
     * @return the first element for specified lock if and only if corresponding response
     * arrives; otherwise, returns null.
     */
    public Response peek(String key) {
        return Optional.ofNullable(this.cachePool.get(key))
                .map(BlockingQueue::peek)
                .orElse(null);
    }

    /**
     * Tries to retrieve the first element from cache queue with specified lock key and remove
     * it from that cache queue.
     *
     * @param key specified key for lock.
     * @return the first element for specified lock if and only if corresponding response
     * arrives; otherwise, returns null.
     */
    public Response take(String key) {
        return Optional.ofNullable(this.cachePool.get(key))
                .map(queue -> {
                    Response response = null;
                    try {
                        response = queue.take();
                    } catch (InterruptedException e) {
                        if (logger.isLoggable(Level.SEVERE)) {
                            logger.log(Level.SEVERE, e.getMessage());
                        }
                        Thread.currentThread().interrupt();
                    }
                    return response;
                }).orElse(null);
    }

    /**
     * Puts a response into the cache pool with specified key from {@link Response}.
     *
     * @param response specified response to be stored.
     */
    public void put(Response response) {
        String key = response.getKey();
        this.cachePool.computeIfAbsent(key,
                k -> new ArrayBlockingQueue<>(ClientProperties.getProperties().getQueueSize()));
        try {
            this.cachePool.get(key).put(response);
        } catch (InterruptedException e) {
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, e.getMessage());
            }
            Thread.currentThread().interrupt();
        }
    }

}
