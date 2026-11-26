package com.crumble_cookies.cream_ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Пул потоков для асинхронного выполнения анализа, запрошенного через REST API. */
@Configuration
public class AsyncConfig {

    @Bean(name = "analysisExecutor")
    public TaskExecutor analysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("analysis-");
        executor.initialize();
        return executor;
    }
}
