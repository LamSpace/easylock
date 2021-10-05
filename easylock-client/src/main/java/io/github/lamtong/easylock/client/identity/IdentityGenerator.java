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

package io.github.lamtong.easylock.client.identity;

/**
 * {@link IdentityGenerator} provides an approach to generates identities for locking and
 * unlocking requests.
 *
 * @author Lam Tong
 * @version 1.3.2
 * @since 1.3.2
 */
public interface IdentityGenerator {

    /**
     * Retrieves an identity of type {@code long}.
     *
     * @return an identity.
     */
    long generate();

}
