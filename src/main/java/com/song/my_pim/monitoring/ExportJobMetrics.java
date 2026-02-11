package com.song.my_pim.monitoring;

import com.song.my_pim.common.constants.MetricsConstants;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@RequiredArgsConstructor
@Component
public class ExportJobMetrics {
    private final MeterRegistry meterRegistry;

    // running gauge: key = phase||jobType||client ("export||articles_xml||12"  →  3)
    // phase = "export" / "upload"
    // job_type="articles_xml" / "articles_json"
    private final Map<String, AtomicInteger> running = new ConcurrentHashMap<>();

    // a wrapper， used in service layer
    public <T> T time(String phase, String jobType, String client, Supplier<T> supplier) {
        Timer.Sample sample = Timer.start(meterRegistry); //sample is an 'unfinished timer'
        incrementRunning(phase, jobType, client);

        try {
            T result = supplier.get();
            record(phase, jobType, client, "success", "none", sample);
            return result;
        } catch (Exception ex) {
            record(phase, jobType, client, "fail", ex.getClass().getSimpleName(), sample);
            throw ex;
        } finally {
            decrementRunning(phase, jobType, client);
        }
    }

    public void time(String phase, String jobType, String client, Runnable runnable) {
        time(phase, jobType, client, () -> {
            runnable.run();
            return null;
        });
    }

    private void record(String phase, String jobType, String client, String result, String exception, Timer.Sample sample) {
        // total counter：success/fail
        Counter.builder(MetricsConstants.EXPORT_JOBS_TOTAL)
                .tags(MetricsConstants.PHASE, phase, MetricsConstants.JOB_TYPE, jobType, MetricsConstants.CLIENT, client, MetricsConstants.RESULT, result, MetricsConstants.EXCEPTION, exception)
                .register(meterRegistry)
                .increment();

        // duration timer
        Timer timer = Timer.builder(MetricsConstants.EXPORT_JOB_DURATION)
                .publishPercentileHistogram()
                .tags(MetricsConstants.PHASE, phase, MetricsConstants.JOB_TYPE, jobType, MetricsConstants.CLIENT, client, MetricsConstants.RESULT, result, MetricsConstants.EXCEPTION, exception)
                .register(meterRegistry);

        sample.stop(timer);
    }

    private void incrementRunning(String phase, String jobType, String client) {
        AtomicInteger runningCount  = running.computeIfAbsent(key(phase, jobType, client), k -> {
            AtomicInteger a = new AtomicInteger(0);
            Gauge.builder(MetricsConstants.EXPORT_JOBS_RUNNING, a, AtomicInteger::get)
                    .tags(MetricsConstants.PHASE, phase, MetricsConstants.JOB_TYPE, jobType, MetricsConstants.CLIENT, client)
                    .register(meterRegistry);
            return a;
        });
        runningCount .incrementAndGet();
    }

    private void decrementRunning(String phase, String jobType, String client) {
        AtomicInteger gauge = running.get(key(phase, jobType, client));
        if (gauge != null) {
            gauge.decrementAndGet();
        }
    }

    private String key(String phase, String jobType, String client) {
        return phase + "||" + jobType + "||" + client;
    }

}
