package com.song.my_pim.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@RequiredArgsConstructor
public class ExportJobThreadPoolConfig {
    private final ExportJobThreadPoolProperties threadPoolProperties;

    @Bean(name = "exportJobThreadPool")
    public TaskExecutor exportJobThreadPool() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(threadPoolProperties.getCorePoolSize());
        ex.setMaxPoolSize(threadPoolProperties.getMaxPoolSize());
        ex.setQueueCapacity(threadPoolProperties.getQueueCapacity());
        ex.setThreadNamePrefix("exportjob-");
        ex.initialize();
        return ex;
    }
}
