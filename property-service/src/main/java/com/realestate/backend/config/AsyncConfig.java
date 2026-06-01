package com.realestate.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Enables {@link org.springframework.scheduling.annotation.Async} support and
 * defines a dedicated executor for geocoding so it can never starve the request
 * thread pool, and so we can size it independently from MVC threads.
 *
 * Pool is intentionally small: Nominatim is rate-limited to ~1 req/sec anyway,
 * so a large pool would just queue work behind the rate limiter.
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    @Bean(name = "geocodingExecutor")
    public Executor geocodingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("geocode-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("Geocoding executor initialized: core=2 max=4 queue=500");
        return executor;
    }
}
