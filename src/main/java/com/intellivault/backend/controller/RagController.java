package com.intellivault.backend.controller;

import com.intellivault.backend.service.SearchService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class RagController {

    private final ChatClient chatClient;
    private final SearchService searchService;

    public RagController(ChatClient.Builder chatClientBuilder, SearchService searchService) {
        this.chatClient = chatClientBuilder.build();
        this.searchService = searchService;
    }

    /**
     * 最终形态的 RAG 接口
     * 调用链路：用户提问 -> 向量检索 -> 构建提示词 -> 大模型推理 -> 返回答案
     */
    @GetMapping("/chat")
    public String chat(@RequestParam String query) {
        // 1. 检索阶段 (Retrieval): 找相关的 3 段话
        var relatedDocs = searchService.search(query, 3);

        // 如果没找到相关信息，直接“幻觉”或者拒答（这里选择坦诚拒答）
        if (relatedDocs.isEmpty()) {
            return "抱歉，知识库中没有找到相关信息。";
        }

        // 2. 提示词工程 (Prompt Engineering): 组装上下文
        String context = relatedDocs.stream()
                .map(doc -> doc.getContent())
                .collect(Collectors.joining("\n---\n"));

        // 定义 RAG 专用提示词模板
        // 核心指令：基于 Context 回答 Question，不要编造。
        String promptText = """
                你是一个智能知识库助手。请根据以下[参考资料]回答用户的问题。
                如果参考资料中没有答案，请直接说“我不知道”，不要编造。
                
                [参考资料]:
                {context}
                
                [用户问题]:
                {question}
                """;

        PromptTemplate promptTemplate = new PromptTemplate(promptText);
        var prompt = promptTemplate.create(Map.of(
                "context", context,
                "question", query
        ));

        // 3. 生成阶段 (Generation): 调用 DeepSeek
        return chatClient.prompt(prompt).call().content();
    }
}