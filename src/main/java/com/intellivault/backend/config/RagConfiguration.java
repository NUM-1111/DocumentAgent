package com.intellivault.backend.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfiguration {

    @Bean
    public ChatMemory chatMemory() {
        // 使用内存存储对话历史，默认容量是 1000 条消息
        return new InMemoryChatMemory();
    }
}