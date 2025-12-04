package com.intellivault.backend.config;

import com.intellivault.backend.memory.RedisChatMemory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RagConfiguration {

    @Bean
    public ChatMemory chatMemory(StringRedisTemplate redisTemplate) {
        // 替换掉原来的 InMemoryChatMemory
        // 传入 RedisTemplate，保留最近 20 条记录
        return new RedisChatMemory(redisTemplate, 20);
    }
}