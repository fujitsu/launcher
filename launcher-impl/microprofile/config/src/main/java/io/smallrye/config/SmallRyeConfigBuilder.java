/*
 * Copyright 2017 Red Hat, Inc.
 * Copyright 2019 Fujitsu Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smallrye.config;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import javax.annotation.Priority;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.eclipse.microprofile.config.spi.Converter;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class SmallRyeConfigBuilder implements ConfigBuilder {

    private static final String META_INF_MICROPROFILE_CONFIG_PROPERTIES = "../../META-INF/microprofile-config.properties";
    private static final String WEB_INF_MICROPROFILE_CONFIG_PROPERTIES = "META-INF/microprofile-config.properties";

    // sources are not sorted by their ordinals
    private List<ConfigSource> sources = new ArrayList<>();
    private Function<ConfigSource, ConfigSource> sourceWrappers = UnaryOperator.identity();
    private Map<Type, ConverterWithPriority> converters = new HashMap<>();
    private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private boolean addDefaultSources = false;
    private boolean addDiscoveredSources = false;
    private boolean addDiscoveredConverters = false;

    public SmallRyeConfigBuilder() {
    }

    @Override
    public ConfigBuilder addDiscoveredSources() {
        addDiscoveredSources = true;
        return this;
    }

    @Override
    public ConfigBuilder addDiscoveredConverters() {
        addDiscoveredConverters = true;
        return this;
    }

    private List<ConfigSource> discoverSources() {
        List<ConfigSource> discoveredSources = new ArrayList<>();
        ServiceLoader<ConfigSource> configSourceLoader = ServiceLoader.load(ConfigSource.class, classLoader);
        configSourceLoader.forEach(discoveredSources::add);

        // load all ConfigSources from ConfigSourceProviders
        ServiceLoader<ConfigSourceProvider> configSourceProviderLoader = ServiceLoader.load(ConfigSourceProvider.class, classLoader);
        configSourceProviderLoader.forEach(configSourceProvider -> {
            configSourceProvider.getConfigSources(classLoader)
                    .forEach(discoveredSources::add);
        });
        return discoveredSources;
    }

    private List<Converter> discoverConverters() {
        List<Converter> converters = new ArrayList<>();
        ServiceLoader<Converter> converterLoader = ServiceLoader.load(Converter.class, classLoader);
        converterLoader.forEach(converters::add);
        return converters;
    }

    @Override
    public ConfigBuilder addDefaultSources() {
        addDefaultSources = true;
        return this;
    }

    private List<ConfigSource> getDefaultSources() {
        List<ConfigSource> defaultSources = new ArrayList<>();

        defaultSources.add(new EnvConfigSource());
        defaultSources.add(new SysPropConfigSource());
        defaultSources.addAll(new PropertiesConfigSourceProvider(META_INF_MICROPROFILE_CONFIG_PROPERTIES, true, classLoader).getConfigSources(classLoader));
        defaultSources.addAll(new PropertiesConfigSourceProvider(WEB_INF_MICROPROFILE_CONFIG_PROPERTIES, true, classLoader).getConfigSources(classLoader));

        return defaultSources;
    }

    @Override
    public ConfigBuilder forClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    @Override
    public ConfigBuilder withSources(ConfigSource... configSources) {
        Collections.addAll(sources, configSources);
        return this;
    }

    @Override
    public ConfigBuilder withConverters(Converter<?>[] converters) {
        for (Converter<?> converter: converters) {
            Type type = Converters.getConverterType(converter.getClass());
            if (type == null) {
                throw new IllegalStateException("Can not add converter " + converter + " that is not parameterized with a type");
            }
            addConverter(type, getPriority(converter), converter, this.converters);
        }
        return this;
    }

    @Override
    public <T> ConfigBuilder withConverter(Class<T> type, int priority, Converter<T> converter) {
        addConverter(type, priority, converter, converters);
        return this;
    }

    // no @Override
    public SmallRyeConfigBuilder withWrapper(UnaryOperator<ConfigSource> wrapper) {
        sourceWrappers = sourceWrappers.andThen(wrapper);
        return this;
    }

    private static void addConverter(Type type, int priority, Converter converter, Map<Type, ConverterWithPriority> converters) {
        // add the converter only if it has a higher priority than another converter for the same type
        ConverterWithPriority oldConverter = converters.get(type);
        int newPriority = getPriority(converter);
        if (oldConverter == null || priority > oldConverter.priority) {
            converters.put(type, new ConverterWithPriority(converter, newPriority));
        }
    }

    private static int getPriority(Converter<?> converter) {
        int priority = 100;
        Priority priorityAnnotation = converter.getClass().getAnnotation(Priority.class);
        if (priorityAnnotation != null) {
            priority = priorityAnnotation.value();
        }
        return priority;
    }

    @Override
    public Config build() {
        final List<ConfigSource> sources = new ArrayList<>(this.sources);
        if (addDiscoveredSources) {
            sources.addAll(discoverSources());
        }
        if (addDefaultSources) {
            sources.addAll(getDefaultSources());
        }

        final Map<Type, ConverterWithPriority> converters = new HashMap<>(this.converters);

        if (addDiscoveredConverters) {
            for(Converter converter : discoverConverters()) {
                Type type = Converters.getConverterType(converter.getClass());
                if (type == null) {
                    throw new IllegalStateException("Can not add converter " + converter + " that is not parameterized with a type");
                }
                addConverter(type, getPriority(converter), converter, converters);
            }
        }

        sources.sort(SmallRyeConfig.CONFIG_SOURCE_COMPARATOR);
        // wrap all
        final Function<ConfigSource, ConfigSource> sourceWrappers = this.sourceWrappers;
        final ListIterator<ConfigSource> it = sources.listIterator();
        while (it.hasNext()) {
            it.set(sourceWrappers.apply(it.next()));
        }

        Map<Type, Converter<?>> configConverters = new HashMap<>();
        converters.forEach((type, converterWithPriority) -> configConverters.put(type, converterWithPriority.converter));
        return newConfig(sources, configConverters);
    }

    protected Config newConfig(List<ConfigSource> sources, Map<Type, Converter<?>> configConverters) {
        ServiceLoader<ConfigFactory> factoryLoader = ServiceLoader.load(ConfigFactory.class, this.classLoader);
        Iterator<ConfigFactory> iter = factoryLoader.iterator();
        if ( !iter.hasNext() ) {
            return new SmallRyeConfig(sources, configConverters);
        }

        ConfigFactory factory = iter.next();
        return factory.newConfig(sources, configConverters);
    }

    private static class ConverterWithPriority {
        private final Converter converter;
        private final int priority;

        private ConverterWithPriority(Converter converter, int priority) {
            this.converter = converter;
            this.priority = priority;
        }
    }

}
