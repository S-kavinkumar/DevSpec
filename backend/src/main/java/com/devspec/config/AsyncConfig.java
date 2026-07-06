package com.devspec.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${devspec.threadpool.core-size:4}")
    private int corePoolSize;

    @Value("${devspec.threadpool.max-size:8}")
    private int maxPoolSize;

    @Value("${devspec.threadpool.queue-capacity:20}")
    private int queueCapacity;

    @Bean(name = "analysisTaskExecutor")
    public Executor analysisTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("DevSpecAsync-");
        executor.initialize();
        return executor;
    }
}
