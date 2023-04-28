package com.fujitsu.launcher.microprofile.telemetry.tracing.cdi;

import com.fujitsu.launcher.microprofile.telemetry.tracing.config.OpenTelemetryConfigProducer;
import com.fujitsu.launcher.microprofile.telemetry.tracing.rest.OpenTelemetryClientFilter;
import com.fujitsu.launcher.microprofile.telemetry.tracing.rest.OpenTelemetryServerFilter;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.util.Nonbinding;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OpenTelemetryExtension implements Extension {

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager) {
        beforeBeanDiscovery.addInterceptorBinding(
                new WithSpanAnnotatedType(beanManager.createAnnotatedType(WithSpan.class)));

        beforeBeanDiscovery.addAnnotatedType(OpenTelemetryConfigProducer.class, OpenTelemetryConfigProducer.class.getName());
        beforeBeanDiscovery.addAnnotatedType(OpenTelemetryProducer.class, OpenTelemetryProducer.class.getName());
        beforeBeanDiscovery.addAnnotatedType(OpenTelemetryServerFilter.class, OpenTelemetryServerFilter.class.getName());
        beforeBeanDiscovery.addAnnotatedType(OpenTelemetryClientFilter.class, OpenTelemetryClientFilter.class.getName());
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {
        afterBeanDiscovery.addBean(new WithSpanInterceptorBean(beanManager));
    }

    // To add Nonbinding to @WithSpan members
    @SuppressWarnings("unchecked")
    static class WithSpanAnnotatedType implements AnnotatedType<WithSpan> {
        private final AnnotatedType<WithSpan> delegate;
        private final Set<AnnotatedMethod<? super WithSpan>> methods;

        WithSpanAnnotatedType(final AnnotatedType<WithSpan> delegate) {
            this.delegate = delegate;
            this.methods = new HashSet<>();

            for (AnnotatedMethod<? super WithSpan> method : delegate.getMethods()) {
                methods.add(new AnnotatedMethod<WithSpan>() {
                    private final AnnotatedMethod<WithSpan> delegate = (AnnotatedMethod<WithSpan>) method;
                    private final Set<Annotation> annotations = Collections.singleton(Nonbinding.Literal.INSTANCE);

                    @Override
                    public Method getJavaMember() {
                        return delegate.getJavaMember();
                    }

                    @Override
                    public List<AnnotatedParameter<WithSpan>> getParameters() {
                        return delegate.getParameters();
                    }

                    @Override
                    public boolean isStatic() {
                        return delegate.isStatic();
                    }

                    @Override
                    public AnnotatedType<WithSpan> getDeclaringType() {
                        return delegate.getDeclaringType();
                    }

                    @Override
                    public Type getBaseType() {
                        return delegate.getBaseType();
                    }

                    @Override
                    public Set<Type> getTypeClosure() {
                        return delegate.getTypeClosure();
                    }

                    @Override
                    public <T extends Annotation> T getAnnotation(final Class<T> annotationType) {
                        if (annotationType.equals(Nonbinding.class)) {
                            return (T) annotations.iterator().next();
                        }
                        return null;
                    }

                    @Override
                    public Set<Annotation> getAnnotations() {
                        return annotations;
                    }

                    @Override
                    public boolean isAnnotationPresent(final Class<? extends Annotation> annotationType) {
                        return annotationType.equals(Nonbinding.class);
                    }
                });
            }
        }

        @Override
        public Class<WithSpan> getJavaClass() {
            return delegate.getJavaClass();
        }

        @Override
        public Set<AnnotatedConstructor<WithSpan>> getConstructors() {
            return delegate.getConstructors();
        }

        @Override
        public Set<AnnotatedMethod<? super WithSpan>> getMethods() {
            return this.methods;
        }

        @Override
        public Set<AnnotatedField<? super WithSpan>> getFields() {
            return delegate.getFields();
        }

        @Override
        public Type getBaseType() {
            return delegate.getBaseType();
        }

        @Override
        public Set<Type> getTypeClosure() {
            return delegate.getTypeClosure();
        }

        @Override
        public <T extends Annotation> T getAnnotation(final Class<T> annotationType) {
            return delegate.getAnnotation(annotationType);
        }

        @Override
        public Set<Annotation> getAnnotations() {
            return delegate.getAnnotations();
        }

        @Override
        public boolean isAnnotationPresent(final Class<? extends Annotation> annotationType) {
            return delegate.isAnnotationPresent(annotationType);
        }
    }
}
