package org.coastline.one.flink.stream.core.source;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.coastline.one.core.TimeTool;
import org.coastline.one.flink.common.model.MonitorData;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author Jay.H.Zou
 * @date 2021/8/29
 */
public class MemorySourceFunction extends RichParallelSourceFunction<MonitorData> {

    private boolean running;
    private Random random;

    public static MemorySourceFunction create() {
        return new MemorySourceFunction();
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        random = new Random(System.currentTimeMillis());
        running = true;

    }

    @Override
    public void run(SourceContext<MonitorData> ctx) throws Exception {
        while (running) {
            MonitorData data = MonitorData.builder()
                    .time(TimeTool.currentTimeMillis())
                    .service("one-flink")
                    .zone("LOCAL")
                    .name("one-name-" + random.nextInt(5))
                    .duration(0)
                    .build();
            TimeUnit.MILLISECONDS.sleep(200);
            ctx.collect(data);
        }
    }

    @Override
    public void cancel() {
        running = false;
    }
}
