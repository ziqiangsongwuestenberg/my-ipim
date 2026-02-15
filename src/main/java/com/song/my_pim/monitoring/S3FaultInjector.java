package com.song.my_pim.monitoring;

import com.song.my_pim.config.S3FaultProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class S3FaultInjector {

    private final S3FaultProperties props;

    @PostConstruct
    void logConfig() { // this method only runs once when the app starts
        log.info("fault enabled={}, failFirstN={}", props.enabled(), props.failFirstN());
    }

    // key -> failed count
    private final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    public S3FaultInjector(S3FaultProperties props) {
        this.props = props;
    }

    public void maybeFail(String key) {
        if (!props.enabled()) return;
        int n = props.failFirstN();
        if (n <= 0) return;

        int attempt = counters.computeIfAbsent(key, k -> new AtomicInteger(0))
                .incrementAndGet();

        if (attempt <= n) {
            throw new RuntimeException("Injected S3 failure for key=" + key + ", attempt=" + attempt);
        }
    }

    public void reset(String key) {
        counters.remove(key);
    }
}
