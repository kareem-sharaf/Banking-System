package com.banking.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;

/**
 * Scheduler Configuration
 * 
 * Configures Spring Scheduler and Async Executor for interest calculation
 * tasks.
 * 
 * @author Banking System
 */
@Configuration
@EnableScheduling
@EnableAsync
@Slf4j
public class SchedulerConfig {

    /**
     * Task Scheduler for scheduled tasks
     * Used for running the daily interest calculation
     */
    @Bean(name = "interestTaskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("interest-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);

        scheduler.setErrorHandler(t -> {
            log.error("Error in scheduled interest calculation task", t);
        });

        log.info("Interest calculation task scheduler configured with pool size: {}", scheduler.getPoolSize());
        return scheduler;
    }

    /**
     * Async Executor for concurrent interest calculations
     * Used for processing multiple accounts in parallel (if needed)
     */
    @Bean(name = "interestCalculationExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("interest-calc-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        log.info("Interest calculation async executor configured - Core: {}, Max: {}",
                executor.getCorePoolSize(), executor.getMaxPoolSize());
        return executor;
    }
}
