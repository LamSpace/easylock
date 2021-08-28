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

package io.github.lamtong.easylock.server.resolver;

import io.github.lamtong.easylock.common.core.Request;
import io.github.lamtong.easylock.common.core.Response;

/**
 * Interface {@link Resolver} resolves requests. Typically, there are two essential steps
 * to resolve requests, namely
 * <ol>
 *     <li>Decides {@code Lock Type} of the requests.</li>
 *     <li>Processed the {@code Lock Request} or {@code Unlock Request}.</li>
 * </ol>
 * Hence, implementations of {@link Resolver} should own these abilities to resolve requests.
 * One of these implementations is {@link RequestResolver}, which dispatches requests according
 * to their {@link Request#type}. And another one is {@link AbstractLockResolver}, which implements
 * {@link Resolver} as well and defines a template to resolve requests for a certain type of lock
 * in {@link AbstractLockResolver#resolve(Request)}.
 *
 * @author Lam Tong
 * @version 1.2.0
 * @see RequestResolver
 * @see AbstractLockResolver
 * @since 1.0.0
 */
public interface Resolver {

    Response resolve(Request request);

}
