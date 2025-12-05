package com.intellivault.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor") // 替换掉 Spring 默认的执行器
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 1. 核心线程数：CPU 核数 + 1 (计算密集型) 或 2 * CPU (IO 密集型)
        // 你的文档解析是 IO (读文件) + CPU (Tika解析) 混合，设为核数 2 倍比较合适
        int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
        executor.setCorePoolSize(corePoolSize);

        // 2. 最大线程数：流量突发时的缓冲
        executor.setMaxPoolSize(corePoolSize * 2);

        // 3. 队列大小：【关键】必须有界！防止 OOM
        // 设为 500 或 1000，意味着最多允许 1000 个任务等待
        executor.setQueueCapacity(500);

        // 4. 线程名称前缀：方便日志排查 (如 "Async-1", "Async-2")
        executor.setThreadNamePrefix("Doc-Async-");

        // 5. 【保命策略】拒绝策略
        // 当队列满了，且线程达到最大值，新任务怎么办？
        // CallerRunsPolicy: 让提交任务的主线程（Controller）自己去执行。
        // 后果：Controller 变慢，无法接受新请求，起到了“由于背压(Backpressure)导致的自然限流”效果，保护了系统不崩。
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        return executor;
    }
}