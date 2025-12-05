package com.intellivault.backend.service;

import com.intellivault.backend.model.KnowledgeDocument;
import com.intellivault.backend.repository.KnowledgeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class) // 使用 Mockito 扩展，不启动 Spring 容器，飞快
class SearchServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private KnowledgeRepository knowledgeRepository;

    @InjectMocks
    private SearchService searchService; // 自动把上面两个 Mock 注入到你要测的 Service 里

    @Test
    @DisplayName("测试核心逻辑：过滤低分文档，并按相似度降序排列")
    void testSearchLogic() {
        // 1. 准备假数据 (Arrange)
        // 假设 Query 的向量是 [1.0, 0.0] (简单的二维向量方便口算)
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{1.0f, 0.0f});

        List<KnowledgeDocument> mockDocs = new ArrayList<>();

        // 文档 A: 向量 [1.0, 0.0] -> 完全重合，余弦相似度应该是 1.0 (最高)
        mockDocs.add(createDoc("Doc_A", 1.0, 0.0));

        // 文档 B: 向量 [0.0, 1.0] -> 垂直，相似度 0.0 -> 应该被过滤掉 (<0.1)
        mockDocs.add(createDoc("Doc_B", 0.0, 1.0));

        // 文档 C: 向量 [0.9, 0.1] -> 夹角很小，相似度很高 (比如 0.9 左右) -> 应该排第二
        mockDocs.add(createDoc("Doc_C", 0.9, 0.1));

        // 文档 D: 向量 [-1.0, 0.0] -> 完全相反，相似度 -1.0 -> 应该被过滤掉
        mockDocs.add(createDoc("Doc_D", -1.0, 0.0));

        when(knowledgeRepository.findAll()).thenReturn(mockDocs);

        // 2. 执行你的代码 (Act)
        // 搜索 "test"，取前 5 个
        List<KnowledgeDocument> results = searchService.search("test", 5);

        // 3. 验证结果 (Assert)

        // 验证 1: 过滤功能 (Doc_B 和 Doc_D 应该被丢掉)
        assertEquals(2, results.size(), "应该只有 Doc_A 和 Doc_C 留下来");

        // 验证 2: 排序功能 (Doc_A 应该在 Doc_C 前面)
        assertEquals("Doc_A", results.get(0).getContent(), "分数最高的应该排第一");
        assertEquals("Doc_C", results.get(1).getContent(), "分数第二的应该排第二");

        // 验证 3: 分数计算 (校验你有没有把 Metadata 写进去)
        Double scoreA = (Double) results.get(0).getMetadata().get("score");
        assertTrue(scoreA > 0.99, "Doc_A 的分数应该是 1.0 左右");

        System.out.println("✅ 测试通过！你的 for 循环逻辑写得很完美！");
    }

    // 辅助方法：快速造文档
    private KnowledgeDocument createDoc(String content, double v1, double v2) {
        return KnowledgeDocument.builder()
                .content(content)
                .embedding(List.of(v1, v2))
                .metadata(new HashMap<>()) // 必须 new 一个 Map，否则 put 会报错
                .build();
    }
}