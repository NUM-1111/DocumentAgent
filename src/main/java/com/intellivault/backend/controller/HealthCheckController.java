package com.intellivault.backend.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthCheckController {

    private final ChatClient chatClient;
    private final MongoTemplate mongoTemplate;

    // Spring AI 1.0.x 推荐使用 ChatClient.Builder
    public HealthCheckController(ChatClient.Builder chatClientBuilder, MongoTemplate mongoTemplate) {
        this.chatClient = chatClientBuilder.build();
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/health")
    public Map<String, String> check() {
        // 1. 测试 AI 连接 (让 DeepSeek 说句话)
        String aiResponse;
        try {
            aiResponse = chatClient.prompt()
                    .user("用一句话证明你还活着，并只输出这句话。")
                    .call()
                    .content();
        } catch (Exception e) {
            aiResponse = "AI 挂了: " + e.getMessage();
        }

        // 2. 测试 DB 连接
        String dbStatus;
        try {
            mongoTemplate.getDb().listCollectionNames().first();
            dbStatus = "Connected to MongoDB: " + mongoTemplate.getDb().getName();
        } catch (Exception e) {
            dbStatus = "DB 挂了: " + e.getMessage();
        }

        return Map.of(
                "ai_status", aiResponse,
                "db_status", dbStatus,
                "app", "IntelliVault-MVP"
        );
    }
}