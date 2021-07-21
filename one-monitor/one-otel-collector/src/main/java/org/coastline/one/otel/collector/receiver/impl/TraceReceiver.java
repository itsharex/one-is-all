package org.coastline.one.otel.collector.receiver.impl;

import io.grpc.BindableService;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import org.coastline.one.otel.collector.config.ReceiverConfig;
import org.coastline.one.otel.collector.processor.DataProcessor;
import org.coastline.one.otel.collector.queue.DataQueue;
import org.coastline.one.otel.collector.receiver.AbstractDataReceiver;

import java.util.List;

/**
 * @author Jay.H.Zou
 * @date 2021/7/20
 */
public class TraceReceiver extends AbstractDataReceiver<ResourceSpans> {

    public TraceReceiver(ReceiverConfig config,
                         List<DataProcessor<ResourceSpans>> processors,
                         DataQueue<ResourceSpans> dataQueue) {
        super(config, processors, dataQueue);
        logger.info("trace receiver start to initialize...");
    }

    public static void create(ReceiverConfig config,
                              List<DataProcessor<ResourceSpans>> processors,
                              DataQueue<ResourceSpans> dataQueue) throws Exception {
        TraceReceiver receiver = new TraceReceiver(config, processors, dataQueue);
        receiver.initialize();
    }

    @Override
    protected BindableService buildService() {
        return new TraceService();
    }

    class TraceService extends TraceServiceGrpc.TraceServiceImplBase {

        @Override
        public void export(ExportTraceServiceRequest request, StreamObserver<ExportTraceServiceResponse> responseObserver) {
            logger.info("span count = {}", request.getResourceSpansCount());
            List<ResourceSpans> resourceSpansList = request.getResourceSpansList();
            logger.info("data = \n{}", resourceSpansList);
            // process
            try {
                resourceSpansList.parallelStream().forEach(resourceSpans -> {
                    boolean result = consume(resourceSpans);
                });
            } catch (Exception e) {
                logger.error("consume trace data error", e);
            }
            ExportTraceServiceResponse response = ExportTraceServiceResponse.getDefaultInstance();
            //ServerCalls.asyncUnimplementedUnaryCall(TraceServiceGrpc.getExportMethod(), responseObserver);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
