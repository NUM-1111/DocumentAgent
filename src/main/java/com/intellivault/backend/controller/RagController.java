package com.intellivault.backend.controller;

import com.intellivault.backend.service.SearchService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux; // [需要 reactive 依赖，见下方]

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@RestController
public class RagController {

    private final ChatClient chatClient;
    private final SearchService searchService;

    // 注入 ChatMemory
    public RagController(ChatClient.Builder chatClientBuilder,
                         SearchService searchService,
                         ChatMemory chatMemory) {
        this.searchService = searchService;

        // [核心改造] 构建带记忆的 ChatClient
        this.chatClient = chatClientBuilder
                .defaultSystem("你是一个专业的知识库助手，请根据提供的参考资料回答问题。")
                // 挂载记忆 Advisor
                .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
                .build();
    }

    /**
     * 带记忆的 RAG 接口
     * 请求示例: /chat?query=它有哪些特性？&userId=user_001
     */
    @GetMapping("/chat")
    public String chat(@RequestParam String query,
                       @RequestParam(defaultValue = "default_user") String userId) {
        // 1. 检索阶段 (Retrieval)
        var relatedDocs = searchService.search(query, 3);

        // 组装上下文 String
        String context = relatedDocs.isEmpty() ? "" : relatedDocs.stream()
                .map(doc -> doc.getContent())
                .collect(Collectors.joining("\n---\n"));

        // 2. 提示词工程 (Prompt Engineering)
        // 注意：这里我们简化提示词，因为 Advisor 会自动把历史记录塞进 Prompt 里
        String promptText = """
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

        // 3. 生成阶段 (Generation)
        return chatClient.prompt(prompt)
                // 传入会话 ID，区分不同用户/会话
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, userId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10) // 只记最近 10 轮
                )
                .call()
                .content();
    }

    /**
     * [新增] 流式对话接口
     * 返回类型是 Flux<String>，配合 produces = TEXT_EVENT_STREAM_VALUE
     * 前端可以像打字机一样一个字一个字接收
     */
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestParam String query,
                                   @RequestParam(defaultValue = "default_user") String userId) {
        // 1. 检索 (Retrieval)
        var relatedDocs = searchService.search(query, 3);

        String context = relatedDocs.isEmpty() ? "" : relatedDocs.stream()
                .map(doc -> doc.getContent())
                .collect(Collectors.joining("\n---\n"));

        // 2. 提示词 (Prompt)
        String promptText = """
                [参考资料]:
                {context}
                
                [用户问题]:
                {question}
                """;

        PromptTemplate promptTemplate = new PromptTemplate(promptText);
        var prompt = promptTemplate.create(Map.of("context", context, "question", query));

        // 3. 流式生成 (Streaming Generation)
        return chatClient.prompt(prompt)
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, userId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10)
                )
                .stream() // [关键] 切换为 stream() 模式
                .content(); // 返回 Flux<String>
    }
}