/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.geronimo.config.cdi;

import org.apache.geronimo.config.DefaultConfigProvider;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.DeploymentException;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.inject.Provider;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:struberg@yahoo.de">Mark Struberg</a>
 */
public class ConfigExtension implements Extension {
    private Config config;

    private static final Predicate<InjectionPoint> NOT_PROVIDERS = ip -> (ip.getType() instanceof Class) || (ip.getType() instanceof ParameterizedType && ((ParameterizedType)ip.getType()).getRawType() != Provider.class);
    private static final Map<Type, Type> REPLACED_TYPES = new HashMap<>();

    static {
        REPLACED_TYPES.put(double.class, Double.class);
        REPLACED_TYPES.put(int.class, Integer.class);
        REPLACED_TYPES.put(float.class, Float.class);
        REPLACED_TYPES.put(long.class, Long.class);
        REPLACED_TYPES.put(boolean.class, Boolean.class);
    }

    private Set<InjectionPoint> injectionPoints = new HashSet<>();


    public void collectConfigProducer(@Observes ProcessInjectionPoint<?, ?> pip) {
        ConfigProperty configProperty = pip.getInjectionPoint().getAnnotated().getAnnotation(ConfigProperty.class);
        if (configProperty != null) {
            injectionPoints.add(pip.getInjectionPoint());
        }
    }

    public void registerConfigProducer(@Observes AfterBeanDiscovery abd, BeanManager bm) {
        Set<Type> types = injectionPoints.stream()
                .filter(NOT_PROVIDERS)
                .map(ip -> REPLACED_TYPES.getOrDefault(ip.getType(), ip.getType()))
                .collect(Collectors.toSet());

        Set<Type> providerTypes = injectionPoints.stream()
                .filter(NOT_PROVIDERS.negate())
                .map(ip -> ((ParameterizedType)ip.getType()).getActualTypeArguments()[0])
                .collect(Collectors.toSet());

        types.addAll(providerTypes);

        types.stream()
                .map(type -> new ConfigInjectionBean(bm, type))
                .forEach(abd::addBean);
    }

    public void validate(@Observes AfterDeploymentValidation add) {
        List<String> deploymentProblems = new ArrayList<>();

        config = ConfigProvider.getConfig();

        for (InjectionPoint injectionPoint : injectionPoints) {
            Type type = injectionPoint.getType();

            // replace native types with their Wrapper types
            type = REPLACED_TYPES.getOrDefault(type, type);

            ConfigProperty configProperty = injectionPoint.getAnnotated().getAnnotation(ConfigProperty.class);
            if (type instanceof Class) {
                // a direct injection of a ConfigProperty
                // that means a Converter must exist.
                String key = ConfigInjectionBean.getConfigKey(injectionPoint, configProperty);
                if ((isDefaultUnset(configProperty.defaultValue()))
                        && !config.getOptionalValue(key, (Class) type).isPresent()) {
                    deploymentProblems.add("No Config Value exists for " + key);
                }
            }
        }

        if (!deploymentProblems.isEmpty()) {
            add.addDeploymentProblem(new DeploymentException("Error while validating Configuration\n"
                                                             + String.join("\n", deploymentProblems)));
        }

    }

    public void shutdown(@Observes BeforeShutdown bsd) {
        DefaultConfigProvider.instance().releaseConfig(config);
    }


    static boolean isDefaultUnset(String defaultValue) {
        return defaultValue.equals(ConfigProperty.UNCONFIGURED_VALUE);
    }
}
