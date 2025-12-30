package net.aahso.homehausen.wallbox_service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class taskExecutorConfig {

    @Bean
    public TaskExecutor taskExecutor() {
            ThreadPoolTaskExecutor t = new ThreadPoolTaskExecutor();
            t.setCorePoolSize(2);
            t.setMaxPoolSize(3);
            t.setThreadNamePrefix("wallbox-datapump-");
            // Ensure the executor attempts to stop tasks on context shutdown
            t.setWaitForTasksToCompleteOnShutdown(true);
            t.setAwaitTerminationSeconds(5);
            // Create daemon threads so JVM can exit if something blocks shutdown
            t.setThreadFactory(r -> {
                Thread th = new Thread(r);
                th.setDaemon(true);
                th.setName("wallbox-datapump-" + System.nanoTime());
                return th;
            });
            t.initialize();
            return t;
    }

}
