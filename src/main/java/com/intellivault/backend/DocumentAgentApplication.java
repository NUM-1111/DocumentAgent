package com.intellivault.backend;

import com.intellivault.backend.service.DocumentService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DocumentAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentAgentApplication.class, args);
    }

    // [新增] 启动时自动检查并灌入数据
    @Bean
    CommandLineRunner initData(DocumentService documentService, com.intellivault.backend.repository.KnowledgeRepository repository) {
        return args -> {
            System.out.println("🧹 正在清理旧数据...");
            repository.deleteAll(); // [关键一步] 先清空，防止旧数据干扰

            System.out.println("🚀 正在注入机密档案...");
            String content = """
                IntelliVault 项目机密档案：
                1. 项目创始人是 [你的名字]。
                2. 项目的核心目标是在 2026年3月 帮助创始人拿到 Java 后端 Offer。
                3. 该系统的最大技术亮点是采用了 "DeepSeek + 本地 ONNX" 的混合架构。
                """;
            documentService.processAndStore(content, "secret_final.txt");

            System.out.println("✅ 数据重置完成！请去浏览器提问。");
        };
    }
}