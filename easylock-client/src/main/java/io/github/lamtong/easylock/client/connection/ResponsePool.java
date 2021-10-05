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

package io.github.lamtong.easylock.client.connection;

import io.github.lamtong.easylock.common.core.Request;
import io.github.lamtong.easylock.common.core.Response;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link ResponsePool} allows threads to take corresponding response from the pool, implemented by
 * {@link ConcurrentHashMap}, waiting if necessary until corresponding response is available.
 * <p>
 * Instance of type {@link SynchronousQueue} is used to transfer the response to corresponding request-send
 * thread. In order to make full use of generated {@link SynchronousQueue}, it is essential to recycle
 * instances which have transfer response to request-send thread.
 *
 * @author Lam Tong
 * @version 1.3.2
 * @since 1.3.2
 */
public final class ResponsePool {

    private static final Logger logger = Logger.getLogger(ResponsePool.class.getName());

    private static final ResponsePool instance = new ResponsePool();

    private final ConcurrentHashMap<Long, BlockingQueue<Response.ResponseProto>> cache = new ConcurrentHashMap<>();

    /**
     * {@code Queues} is used to cache {@link BlockingQueue} which is used to transfer response and response has
     * been retrieved.
     */
    private final ConcurrentLinkedQueue<BlockingQueue<Response.ResponseProto>> queues = new ConcurrentLinkedQueue<>();

    private ResponsePool() {
    }

    public static ResponsePool getInstance() {
        return instance;
    }

    /**
     * Retrieves a response with corresponding request, waiting if necessary until the response is available.
     *
     * @param request corresponding request
     * @return a response, blocked until the response if available.
     */
    public Response.ResponseProto take(Request.RequestProto request) {
        long identity = request.getIdentity();
        BlockingQueue<Response.ResponseProto> queue = this.queues.poll();
        if (queue == null) {
            // No cached blocking queue instance, adds a new one.
            this.cache.put(identity, new SynchronousQueue<>());
        } else {
            this.cache.put(identity, queue);
        }
        Response.ResponseProto response = null;
        try {
            // Retrieves the response.
            response = this.cache.get(identity).take();
            // Recycles the synchronous queue instance.
            this.queues.offer(this.cache.remove(identity));
        } catch (InterruptedException e) {
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, e.getMessage());
            }
            Thread.currentThread().interrupt();
        }
        return response;
    }

    /**
     * Puts a response into the cache.
     *
     * @param response response to be put into the cache.
     */
    public void put(Response.ResponseProto response) {
        long identity = response.getIdentity();
        //noinspection StatementWithEmptyBody,LoopConditionNotUpdatedInsideLoop
        while (!this.cache.containsKey(identity)) {
            // Wait to avoid NullPointerException
        }
        try {
            this.cache.get(identity).put(response);
        } catch (InterruptedException e) {
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, e.getMessage());
            }
            Thread.currentThread().interrupt();
        }
    }

}
