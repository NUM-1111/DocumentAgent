package com.intellivault.backend.memory;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 手写 Redis 记忆存储
 * 亮点：利用 Redis List 数据结构存储对话历史，设置 TTL 自动过期
 */
public class RedisChatMemory implements ChatMemory {

    private final StringRedisTemplate redisTemplate;
    private final int retentionSize; // 只保留最近 N 条

    public RedisChatMemory(StringRedisTemplate redisTemplate, int retentionSize) {
        this.redisTemplate = redisTemplate;
        this.retentionSize = retentionSize;
    }

    private String getKey(String conversationId) {
        return "chat:memory:" + conversationId;
    }

    @Override
    public void add(@NonNull String conversationId, @NonNull List<Message> messages) {
        String key = getKey(conversationId);
        for (Message msg : messages) {
            // 简单序列化：Type|Content
            String value = msg.getMessageType().name() + "|" + msg.getContent();
            redisTemplate.opsForList().rightPush(key, value);
        }
        // 截断历史，防止无限增长 (保留最后 retentionSize 条)
        redisTemplate.opsForList().trim(key, -retentionSize, -1);
        // 设置 1 小时过期，节省空间
        redisTemplate.expire(key, 60, java.util.concurrent.TimeUnit.MINUTES);
    }

    @Override
    public List<Message> get(@NonNull String conversationId, int lastN) {
        String key = getKey(conversationId);
        // 取出所有历史
        List<String> rawData = redisTemplate.opsForList().range(key, 0, -1);
        if (rawData == null) return new ArrayList<>();

        return rawData.stream()
                .map(this::deserialize)
                .collect(Collectors.toList());
    }

    @Override
    public void clear(@NonNull String conversationId) {
        redisTemplate.delete(getKey(conversationId));
    }

    // 反序列化辅助方法
    private Message deserialize(String raw) {
        int splitIndex = raw.indexOf("|");
        String type = raw.substring(0, splitIndex);
        String content = raw.substring(splitIndex + 1);

        switch (type) {
            case "USER": return new UserMessage(content);
            case "ASSISTANT": return new AssistantMessage(content);
            case "SYSTEM": return new SystemMessage(content);
            default: return new UserMessage(content);
        }
    }
}