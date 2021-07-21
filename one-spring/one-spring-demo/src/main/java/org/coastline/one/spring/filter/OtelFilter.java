package org.coastline.one.spring.filter;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.exporter.otlp.internal.SpanAdapter;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.coastline.one.spring.config.OTelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * @author Jay.H.Zou
 * @date 2021/7/19
 */
public class OtelFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(OtelFilter.class);

    private static final String SPAN_NAME_URL = "monitor_provider";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        String method = request.getMethod();
        String requestURI = request.getRequestURI();
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        int status = response.getStatus();
        logger.info("method = {}, url = {}, code = {}", method, requestURI, status);
        Tracer tracer = OTelConfig.getTracer();
        Span span = tracer.spanBuilder(SPAN_NAME_URL).setSpanKind(SpanKind.PRODUCER).startSpan();

        try (Scope scope = span.makeCurrent()) {
            // your use case
            Attributes attributes = Attributes.of(AttributeKey.stringKey("http_method"), method,
                    AttributeKey.stringKey("http_url"), requestURI,
                    AttributeKey.stringKey("http_code"), String.valueOf(status));
            span.setAllAttributes(attributes);
            span.addEvent("test_event");
            span.addEvent("event_2", Instant.now());

            Span childSpan = tracer.spanBuilder("child").startSpan();
            try (Scope scopeChildren = childSpan.makeCurrent()) {
                System.out.println("build child span");
            } finally {
                childSpan.end();
            }
        } catch (Exception t) {
            span.setStatus(StatusCode.ERROR, t.getMessage());
        } finally {
            span.end(); // closing the scope does not end the span, this has to be done manually
        }
        customSend((ReadWriteSpan)span);
        filterChain.doFilter(servletRequest,servletResponse);
    }

    private void customSend(ReadWriteSpan span) {
        List<SpanData> spans = Collections.singletonList(span.toSpanData());
        // 构建客户端
        ExportTraceServiceRequest exportTraceServiceRequest = ExportTraceServiceRequest.newBuilder()
                .addAllResourceSpans(SpanAdapter.toProtoResourceSpans(spans))
                .build();
        ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("localhost", 4317).usePlaintext().build();

        TraceServiceGrpc.TraceServiceFutureStub exporter = TraceServiceGrpc.newFutureStub(managedChannel);
        Futures.addCallback(
                exporter.export(exportTraceServiceRequest),
                new FutureCallback<ExportTraceServiceResponse>() {
                    @Override
                    public void onSuccess(@Nullable ExportTraceServiceResponse response) {
                        System.out.println("res: " + response);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        System.out.println(t);
                    }
                },
                MoreExecutors.directExecutor());
        managedChannel.shutdown();
    }
}