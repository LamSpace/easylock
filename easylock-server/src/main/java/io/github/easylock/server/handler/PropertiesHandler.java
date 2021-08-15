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

package io.github.easylock.server.handler;

import io.github.easylock.server.property.ServerProperties;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link PropertiesHandler} handles properties when initialization. Generally, {@link ServerProperties}
 * provides basic properties to launch the lock server while those properties can be overridden.
 * <p>
 * <b>Override of Properties</b>
 * <p>
 * By default, <code>server properties</code> originates from three different ways, which are
 * <ol>
 *     <li>From {@link ServerProperties};</li>
 *     <li>From file named server.properties in classpath;</li>
 *     <li>From <code>command line</code>.</li>
 * </ol>
 * <p>
 * And <code>properties</code> override each other from the bottom to the top in the list before. In other
 * words, properties from <code>command line</code> has the highest priority and properties from
 * {@link ServerProperties} has the lowest priority.
 *
 * @author Lam Tong
 * @version 1.0.0
 * @since 1.0.0
 */
public final class PropertiesHandler {

    private static final Logger logger = Logger.getLogger(PropertiesHandler.class.getName());

    public void handleProperties(String[] args) {
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "Properties handled - {0}", Arrays.toString(args));
        }
    }

}
