package com.intellivault.backend;

import com.intellivault.backend.service.DocumentService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync; // [新增]

@EnableAsync
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

            System.out.println("✅ 数据重置完成！请去浏览器提问。");
        };
    }
}