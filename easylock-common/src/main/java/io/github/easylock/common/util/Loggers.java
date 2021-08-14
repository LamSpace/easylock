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

package io.github.easylock.common.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>Logger</code> tools, using {@link Logger}, to record logs for both client and server.
 *
 * @author Lam Tong
 * @version 1.0.0
 * @since 1.0.0
 */
public final class Loggers {

    private Loggers() {
    }

    /**
     * Prints logs via {@link Logger} with specified log {@link Level} and message.
     *
     * @param logger  {@link Logger} object to print.
     * @param level   log level.
     * @param message specified message to print.
     */
    public static void log(Logger logger, Level level, String message) {
        if (logger.isLoggable(level)) {
            logger.log(level, message);
        }
    }

}
