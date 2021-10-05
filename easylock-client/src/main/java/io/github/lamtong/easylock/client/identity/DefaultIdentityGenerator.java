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

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Default implementation of {@link IdentityGenerator} via {@link AtomicLong} to provide unique identities for
 * requests.
 *
 * @author Lam Tong
 * @version 1.3.2
 * @see IdentityGenerator
 * @since 1.3.2
 */
public final class DefaultIdentityGenerator implements IdentityGenerator {

    @SuppressWarnings(value = {"unused"})
    private static final Logger logger = Logger.getLogger(DefaultIdentityGenerator.class.getName());

    private static final IdentityGenerator instance = new DefaultIdentityGenerator();

    private final AtomicLong count = new AtomicLong();

    private DefaultIdentityGenerator() {
    }

    public static IdentityGenerator getInstance() {
        return instance;
    }

    @Override
    public long generate() {
        return count.incrementAndGet();
    }

}
