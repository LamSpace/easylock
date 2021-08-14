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

package io.github.easylock.autoconfigure;

import io.github.easylock.autoconfigure.aspect.SimpleLockAspect;
import io.github.easylock.client.property.ClientProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Autoconfiguration for <code>easylock-client</code>.
 * <p>
 * Since {@link ClientProperties} already has default values, {@link EasyLockAutoConfiguration}
 * <b>inject</b> properties after {@link EasyLockAutoConfiguration} has been initialized via
 * annotation {@link PostConstruct}.
 * <p>
 * <b>Usage of {@link Import}</b>
 * <p>
 * In order to configure <code>Aspect</code>s to process annotations provided by this starter,
 * {@link Import} is used to activate annotations like {@link SimpleLockAspect}.
 *
 * @author Lam Tong
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
@Import(value = {SimpleLockAspect.class})
@EnableConfigurationProperties(value = EasyLockProperties.class)
@ConditionalOnClass(value = EasyLockProperties.class)
public class EasyLockAutoConfiguration {

    private static final Logger logger = Logger.getLogger(EasyLockAutoConfiguration.class.getName());

    @Autowired
    private EasyLockProperties properties;

    @Autowired
    private Environment environment;

    @PostConstruct
    public void setClientProperties() {
        ClientProperties clientProperties = ClientProperties.getProperties();
        Class<? extends ClientProperties> clazz = clientProperties.getClass();
        try {
            Field application = clazz.getDeclaredField("application");
            application.setAccessible(true);
            String property = this.environment.getProperty("spring.application.name");
            if (property != null) {
                application.set(clientProperties, property);
            } else {
                application.set(clientProperties, UUID.randomUUID().toString());
            }

            Field host = clazz.getDeclaredField("host");
            host.setAccessible(true);
            host.set(clientProperties, this.properties.getServerHost());

            Field port = clazz.getDeclaredField("port");
            port.setAccessible(true);
            port.set(clientProperties, this.properties.getServerPort());

            Field connections = clazz.getDeclaredField("connections");
            connections.setAccessible(true);
            connections.set(clientProperties, this.properties.getChannelConnections());

            Field queueSize = clazz.getDeclaredField("queueSize");
            queueSize.setAccessible(true);
            queueSize.set(clientProperties, this.properties.getCacheQueueSize());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, e.getMessage());
            }
        }
    }

}