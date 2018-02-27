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
package org.apache.geronimo.config;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.eclipse.microprofile.config.spi.Converter;
import org.apache.geronimo.config.configsource.PropertyFileConfigSourceProvider;
import org.apache.geronimo.config.configsource.SystemEnvConfigSource;
import org.apache.geronimo.config.configsource.SystemPropertyConfigSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;

import javax.enterprise.inject.Typed;
import javax.enterprise.inject.Vetoed;

import static java.util.Arrays.asList;

/**
 * @author <a href="mailto:rmannibucau@apache.org">Romain Manni-Bucau</a>
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 */
@Typed
@Vetoed
public class DefaultConfigBuilder implements ConfigBuilder {
    protected ClassLoader forClassLoader;
    private final List<ConfigSource> sources = new ArrayList<>();
    private final List<Converter<?>> converters = new ArrayList<>();
    private boolean ignoreDefaultSources = true;
    private boolean ignoreDiscoveredSources = true;
    private boolean ignoreDiscoveredConverters = true;

    @Override
    public ConfigBuilder addDefaultSources() {
        this.ignoreDefaultSources = false;
        return this;
    }

    @Override
    public ConfigBuilder addDiscoveredSources() {
        this.ignoreDiscoveredSources = false;
        return this;
    }

    @Override
    public ConfigBuilder forClassLoader(final ClassLoader loader) {
        this.forClassLoader = loader;
        return this;
    }

    @Override
    public ConfigBuilder withSources(final ConfigSource... sources) {
        this.sources.addAll(asList(sources));
        return this;
    }

    @Override
    public ConfigBuilder withConverters(Converter<?>... converters) {
        this.converters.addAll(asList(converters));
        return this;
    }

    @Override
    public ConfigBuilder addDiscoveredConverters() {
        ignoreDiscoveredConverters = false;
        return this;
    }

    @Override
    public Config build() {
        List<ConfigSource> configSources = new ArrayList<>();
         if (forClassLoader == null) {
             forClassLoader = Thread.currentThread().getContextClassLoader();
             if (forClassLoader == null) {
                 forClassLoader = DefaultConfigProvider.class.getClassLoader();
             }
         }

        if (!ignoreDefaultSources) {
            configSources.addAll(getBuiltInConfigSources(forClassLoader));
        }
        configSources.addAll(sources);

        if (!ignoreDiscoveredSources) {
            // load all ConfigSource services
            ServiceLoader<ConfigSource> configSourceLoader = ServiceLoader.load(ConfigSource.class, forClassLoader);
            configSourceLoader.forEach(configSources::add);

            // load all ConfigSources from ConfigSourceProviders
            ServiceLoader<ConfigSourceProvider> configSourceProviderLoader = ServiceLoader.load(ConfigSourceProvider.class, forClassLoader);
            configSourceProviderLoader.forEach(configSourceProvider ->
                    configSourceProvider.getConfigSources(forClassLoader)
                            .forEach(configSources::add));
        }

        if (!ignoreDiscoveredConverters) {
            ServiceLoader<Converter> converterLoader = ServiceLoader.load(Converter.class, forClassLoader);
            converterLoader.forEach(converters::add);
        }

        ConfigImpl config = new ConfigImpl();
        config.addConfigSources(configSources);

        for (Converter<?> converter : converters) {
            config.addConverter(converter);
        }

        return config;
    }

    protected Collection<? extends ConfigSource> getBuiltInConfigSources(ClassLoader forClassLoader) {
        List<ConfigSource> configSources = new ArrayList<>();

        configSources.add(new SystemEnvConfigSource());
        configSources.add(new SystemPropertyConfigSource());
        configSources.addAll(new PropertyFileConfigSourceProvider("META-INF/microprofile-config.properties", true, forClassLoader).getConfigSources(forClassLoader));

        return configSources;
    }
}
