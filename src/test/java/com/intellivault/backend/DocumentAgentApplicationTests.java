package com.intellivault.backend;

import com.intellivault.backend.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        // 注入假 Key 绕过检查
        "spring.ai.openai.api-key=sk-dummy-key-for-test-only",
        "spring.ai.openai.base-url=https://api.deepseek.com",

        // [关键修正] 明确指定使用 transformers 作为嵌入模型实现
        "spring.ai.model.embedding=transformers",

        // [关键修正] 修正属性前缀，强制测试环境使用本地文件
        "spring.ai.embedding.transformer.tokenizer.uri=classpath:/onnx/all-MiniLM-L6-v2/tokenizer.json",
        "spring.ai.embedding.transformer.onnx.model-uri=classpath:/onnx/all-MiniLM-L6-v2/model.onnx"
})
class DocumentAgentApplicationTests {

    @Autowired
    private DocumentService documentService;

    @Test
    void testIngestion() {
        String sourceFilename = "interview_guide.txt";
        String content = "IntelliVault 本地向量化引擎测试文本。";

        System.out.println("🚀 开始执行文档入库测试...");
        documentService.processAndStore(content, sourceFilename);
        System.out.println("✅ 测试结束，请检查 MongoDB。");
    }

    @Autowired
    private com.intellivault.backend.service.SearchService searchService;

    @Test
    void testSearch() {
        // 1. 确保库里有数据 (先跑一次入库)
        String content = "Spring AI 支持多种大模型，包括 OpenAI, Azure, DeepSeek 等。IntelliVault 是一个基于 Spring AI 的项目。";
        documentService.processAndStore(content, "search_test_doc.txt");

        // 2. 模拟搜索
        String query = "IntelliVault 是基于什么框架的？";
        System.out.println("🔍 正在搜索: " + query);

        var results = searchService.search(query, 3);

        // 3. 打印结果
        results.forEach(doc -> {
            System.out.println("------------------------------------------------");
            System.out.println("📝 匹配片段: " + doc.getContent());
            System.out.println("🎯 相似度分: " + doc.getMetadata().get("score"));
        });
    }

    @Test
    void testRealDataIngestion() {
        // 这是一段 DeepSeek 训练数据里绝对没有的“私有知识”
        // 如果 AI 能回答出来，说明它真的读了你的库！
        String content = """
            IntelliVault 项目机密：
            1. 项目创始人是 Num-1111。
            2. 项目的核心目标是在 2026年3月 帮助创始人拿到 Java 后端 Offer。
            3. 该系统的最大技术亮点是采用了 "DeepSeek + 本地 ONNX" 的混合架构。
            """;

        // 入库 (注意文件名换一个，避免和之前的混淆)
        documentService.processAndStore(content, "secret_project_info.txt");
    }
}