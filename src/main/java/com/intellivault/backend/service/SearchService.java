package com.intellivault.backend.service;

import com.intellivault.backend.model.KnowledgeDocument;
import com.intellivault.backend.repository.KnowledgeRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private final EmbeddingModel embeddingModel;
    private final KnowledgeRepository knowledgeRepository;

    public SearchService(EmbeddingModel embeddingModel, KnowledgeRepository knowledgeRepository) {
        this.embeddingModel = embeddingModel;
        this.knowledgeRepository = knowledgeRepository;
    }

    /**
     * 核心功能：语义搜索
     * @param query 用户的问题，例如 "IntelliVault 的核心技术是什么？"
     * @param topK 返回最相似的几条结果
     */
    public List<KnowledgeDocument> search(String query, int topK) {
        // 1. 把用户的问题也变成向量 (Query Embedding)
        // 注意：这里必须转换类型，跟入库时保持一致
        float[] queryEmbeddingArray = embeddingModel.embed(query);
        List<Double> queryVector = convertFloatArrayToList(queryEmbeddingArray);

        // 2. 取出库里所有文档 (MVP阶段策略：全量取出在内存计算)
        // 面试话术：对于百万级以下数据，内存计算比IO更像瓶颈；海量数据可升级为 PGVector 或 Mongo Atlas
        List<KnowledgeDocument> allDocs = knowledgeRepository.findAll();

        // 3. 内存计算相似度并排序
        return allDocs.stream()
                .map(doc -> {
                    // 计算相似度分数
                    double score = cosineSimilarity(queryVector, doc.getEmbedding());
                    doc.getMetadata().put("score", score); // 把分数暂存到 metadata 方便查看
                    return doc;
                })
                // 过滤掉完全不相关的 (可选，这里设个阈值 0.5)
                .filter(doc -> (double) doc.getMetadata().get("score") > 0.1)
                // 按分数降序排列 (最像的排前面)
                .sorted(Comparator.comparingDouble((KnowledgeDocument doc) ->
                        (Double) doc.getMetadata().get("score")).reversed())
                .peek(doc -> System.out.println("🔍 候选文档得分: " + doc.getMetadata().get("score") + " | 内容: " + doc.getContent().substring(0, Math.min(20, doc.getContent().length())))) // [修改点] 打印日志调试
                .limit(topK)
                .collect(Collectors.toList());
    }

    // 辅助工具：float[] 转 List<Double>
    private List<Double> convertFloatArrayToList(float[] array) {
        List<Double> list = new java.util.ArrayList<>(array.length);
        for (float f : array) {
            list.add((double) f);
        }
        return list;
    }

    /**
     * 数学核心：余弦相似度计算
     * 面试手写算法题级别的高频考点
     */
    private double cosineSimilarity(List<Double> v1, List<Double> v2) {
        if (v1 == null || v2 == null || v1.size() != v2.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < v1.size(); i++) {
            dotProduct += v1.get(i) * v2.get(i);
            normA += Math.pow(v1.get(i), 2);
            normB += Math.pow(v2.get(i), 2);
        }

        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}