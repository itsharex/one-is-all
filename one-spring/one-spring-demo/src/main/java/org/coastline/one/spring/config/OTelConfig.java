package org.coastline.one.spring.config;

import com.google.common.collect.Lists;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.GlobalMeterProvider;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.aggregator.AggregatorFactory;
import io.opentelemetry.sdk.metrics.common.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.export.IntervalMetricReader;
import io.opentelemetry.sdk.metrics.view.InstrumentSelector;
import io.opentelemetry.sdk.metrics.view.View;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;

import java.util.Collections;

/**
 * @author Jay.H.Zou
 * @date 2021/7/19
 */
public class OTelConfig {

    private static final String TRACES_ENDPOINT_URL = "http://localhost:4317";

    private static final String METRICS_ENDPOINT_URL = "http://localhost:4318";

    private static OpenTelemetry openTelemetry;

    private static SdkMeterProvider meterProvider;

    private static final Resource resource = Resource.create(
            Attributes.of(
                    AttributeKey.stringKey("service.name"), "one-spring-demo",
                    AttributeKey.stringKey("service.zone"), "LOCAL")
    );

    private static void initTracer() {
        OtlpGrpcSpanExporter exporter = OtlpGrpcSpanExporter.builder().setEndpoint(TRACES_ENDPOINT_URL).build();
        // span 处理器
        BatchSpanProcessor spanProcessor = BatchSpanProcessor.builder(exporter).build();
        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(spanProcessor)
                .addSpanProcessor(BatchSpanProcessor.builder(OtlpGrpcSpanExporter.builder().build()).build())
                .build();
        openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();
    }


    private static void initMeter() {
        Resource resource = Resource.create(
                Attributes.of(
                        AttributeKey.stringKey("service.name"), "one-spring-demo",
                        AttributeKey.stringKey("service.zone"), "LOCAL")
        );
        OtlpGrpcMetricExporter exporter = OtlpGrpcMetricExporter.builder()
                .setEndpoint(METRICS_ENDPOINT_URL).build();

        InstrumentSelector instrumentSelector = InstrumentSelector.builder()
                .setInstrumentType(InstrumentType.VALUE_RECORDER).build();
        View view = View.builder()
                .setAggregatorFactory(AggregatorFactory.histogram(Lists.newArrayList(1D, 10D, 50D, 100D), AggregationTemporality.DELTA))
                .build();
        meterProvider = SdkMeterProvider.builder()
                .setResource(resource)
                .registerView(instrumentSelector, view)
                .buildAndRegisterGlobal();
        IntervalMetricReader.builder()
                .setMetricProducers(Collections.singleton((SdkMeterProvider) GlobalMeterProvider.get()))
                .setMetricExporter(exporter)
                .setExportIntervalMillis(10000)// configurable interval
                .build().start();
    }

    static {
        initTracer();
        initMeter();
        //OpenTelemetrySdkAutoConfiguration.initialize();
    }

    public static Meter getMeter() {
        return meterProvider.get("otel-sdk", "1.4.1");
    }

    public static Tracer getTracer() {
        return openTelemetry.getTracer("otel-sdk", "1.4.1");
    }
}
