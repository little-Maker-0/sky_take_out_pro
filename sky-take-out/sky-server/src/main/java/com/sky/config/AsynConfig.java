package com.sky.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsynConfig {

    @Value("${sky.async.core-pool-multiplier}")
    private int corePoolMultiplier;

    @Value("${sky.async.max-pool-multiplier}")
    private int maxPoolMultiplier;

    @Value("${sky.async.queue-capacity}")
    private int queueCapacity;

    @Value("${sky.async.keep-alive-seconds}")
    private int keepAliveSeconds;

    @Value("${sky.async.await-termination-seconds}")
    private int awaitTerminationSeconds;

    @Value("${sky.async.thread-name-prefix}")
    private String threadNamePrefix;

    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        int processorCount = Runtime.getRuntime().availableProcessors();
        taskExecutor.setCorePoolSize(processorCount * corePoolMultiplier);
        taskExecutor.setMaxPoolSize(processorCount * maxPoolMultiplier);
        taskExecutor.setQueueCapacity(queueCapacity);
        taskExecutor.setThreadNamePrefix(threadNamePrefix);
        taskExecutor.setKeepAliveSeconds(keepAliveSeconds);
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        taskExecutor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        taskExecutor.initialize();
        return taskExecutor;
    }

    @Bean("applicationTaskExecutor")
    SimpleAsyncTaskExecutor applicationTaskExecutor() {
        return new SimpleAsyncTaskExecutor("app-");
    }
}
