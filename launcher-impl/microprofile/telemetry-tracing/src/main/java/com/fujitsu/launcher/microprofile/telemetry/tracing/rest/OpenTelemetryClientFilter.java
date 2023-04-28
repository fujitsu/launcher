package com.fujitsu.launcher.microprofile.telemetry.tracing.rest;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesGetter;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanStatusExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.net.NetClientAttributesGetter;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

import static com.fujitsu.launcher.microprofile.telemetry.tracing.config.OpenTelemetryConfig.INSTRUMENTATION_NAME;
import static com.fujitsu.launcher.microprofile.telemetry.tracing.config.OpenTelemetryConfig.INSTRUMENTATION_VERSION;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@Provider
public class OpenTelemetryClientFilter implements ClientRequestFilter, ClientResponseFilter {
    private Instrumenter<ClientRequestContext, ClientResponseContext> instrumenter;

    // RESTEasy requires no-arg constructor for CDI injection: https://issues.redhat.com/browse/RESTEASY-1538
    public OpenTelemetryClientFilter() {
    }

    @Inject
    public OpenTelemetryClientFilter(final OpenTelemetry openTelemetry) {
        ClientAttributesExtractor clientAttributesExtractor = new ClientAttributesExtractor();

        // TODO - The Client Span name is only "HTTP {METHOD_NAME}": https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/semantic_conventions/http.md#name
        InstrumenterBuilder<ClientRequestContext, ClientResponseContext> builder = Instrumenter.builder(
                openTelemetry,
                INSTRUMENTATION_NAME,
                HttpSpanNameExtractor.create(clientAttributesExtractor));
        builder.setInstrumentationVersion(INSTRUMENTATION_VERSION);

        this.instrumenter = builder
                .setSpanStatusExtractor(HttpSpanStatusExtractor.create(clientAttributesExtractor))
                .addAttributesExtractor(
                        HttpClientAttributesExtractor.create(clientAttributesExtractor, new NetClientAttributesExtractor()))
                .buildClientInstrumenter(new ClientRequestContextTextMapSetter());
    }

    @Override
    public void filter(final ClientRequestContext request) {
        // CDI is not available in some contexts even if this library is available on the CP
        if (instrumenter != null) {
            Context parentContext = Context.current();
            if (instrumenter.shouldStart(parentContext, request)) {
                Context spanContext = instrumenter.start(parentContext, request);
                Scope scope = spanContext.makeCurrent();
                request.setProperty("otel.span.client.context", spanContext);
                request.setProperty("otel.span.client.parentContext", parentContext);
                request.setProperty("otel.span.client.scope", scope);
            }
        }
    }

    @Override
    public void filter(final ClientRequestContext request, final ClientResponseContext response) {
        // CDI is not available in some contexts even if this library is available on the CP
        if (instrumenter != null) {
            Scope scope = (Scope) request.getProperty("otel.span.client.scope");
            if (scope == null) {
                return;
            }

            Context spanContext = (Context) request.getProperty("otel.span.client.context");
            try {
                instrumenter.end(spanContext, request, response, null);
            } finally {
                scope.close();

                request.removeProperty("otel.span.client.context");
                request.removeProperty("otel.span.client.parentContext");
                request.removeProperty("otel.span.client.scope");
            }
        }
    }

    private static class ClientRequestContextTextMapSetter implements TextMapSetter<ClientRequestContext> {
        @Override
        public void set(final ClientRequestContext carrier, final String key, final String value) {
            if (carrier != null) {
                carrier.getHeaders().put(key, singletonList(value));
            }
        }
    }

    private static class ClientAttributesExtractor
            implements HttpClientAttributesGetter<ClientRequestContext, ClientResponseContext> {

        @Override
        public String getUrl(final ClientRequestContext request) {
            return request.getUri().toString();
        }

        @Override
        public String getFlavor(final ClientRequestContext request, final ClientResponseContext response) {
            return null;
        }

        @Override
        public String getMethod(final ClientRequestContext request) {
            return request.getMethod();
        }

        @Override
        public List<String> getRequestHeader(final ClientRequestContext request, final String name) {
            return request.getStringHeaders().getOrDefault(name, emptyList());
        }

        @Override
        public Integer getStatusCode(final ClientRequestContext request, final ClientResponseContext response,
                final Throwable throwable) {
            return response.getStatus();
        }

        @Override
        public List<String> getResponseHeader(final ClientRequestContext request, final ClientResponseContext response,
                final String name) {
            return response.getHeaders().getOrDefault(name, emptyList());
        }
    }

    private static class NetClientAttributesExtractor
            implements NetClientAttributesGetter<ClientRequestContext, ClientResponseContext> {
        @Override
        public String getTransport(
                final ClientRequestContext clientRequestContext,
                final ClientResponseContext clientResponseContext) {
            return null;
        }

        @Override
        public String getPeerName(final ClientRequestContext clientRequestContext) {
            return null;
        }

        @Override
        public Integer getPeerPort(final ClientRequestContext clientRequestContext) {
            return null;
        }

        @Override
        public String getSockFamily(
                final ClientRequestContext clientRequestContext,
                final ClientResponseContext clientResponseContext) {
            return null;
        }

        @Override
        public String getSockPeerAddr(
                final ClientRequestContext clientRequestContext,
                final ClientResponseContext clientResponseContext) {
            return null;
        }

        @Override
        public String getSockPeerName(
                final ClientRequestContext clientRequestContext,
                final ClientResponseContext clientResponseContext) {
            return null;
        }

        @Override
        public Integer getSockPeerPort(
                final ClientRequestContext clientRequestContext,
                final ClientResponseContext clientResponseContext) {
            return null;
        }
    }
}
