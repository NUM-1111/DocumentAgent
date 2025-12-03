package com.intellivault.backend.service;

import com.intellivault.backend.model.KnowledgeDocument;
import com.intellivault.backend.repository.KnowledgeRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream; // 引入这个

@Service
public class DocumentService {

    private final EmbeddingModel embeddingModel;
    private final KnowledgeRepository knowledgeRepository;

    public DocumentService(EmbeddingModel embeddingModel, KnowledgeRepository knowledgeRepository) {
        this.embeddingModel = embeddingModel;
        this.knowledgeRepository = knowledgeRepository;
    }

    public void processAndStore(String content, String sourceFilename, String fileId) {
        if (!StringUtils.hasText(content)) return;

        // 1. 文本切片
        var splitter = new TokenTextSplitter();
        List<Document> splitDocuments = splitter.apply(List.of(new Document(content)));

        // 2. 向量化处理 & 实体转换
        List<KnowledgeDocument> knowledgeDocs = splitDocuments.parallelStream()
                .map(chunk -> {
                    // [修复点]：接收 float[] 数组
                    float[] embeddingArray = embeddingModel.embed(chunk.getContent());

                    // [修复点]：将 float[] 转换为 List<Double>
                    // 使用 IntStream 进行高效转换，或者用普通的 for 循环
                    List<Double> vector = new ArrayList<>(embeddingArray.length);
                    for (float f : embeddingArray) {
                        vector.add((double) f);
                    }

                    // 构建实体
                    return KnowledgeDocument.builder()
                            .content(chunk.getContent())
                            .embedding(vector)
                            .sourceFilename(sourceFilename)
                            .fileId(fileId)
                            .metadata(Map.of("chunk_index", splitDocuments.indexOf(chunk)))
                            .build();
                })
                .collect(Collectors.toList());

        // 3. 批量入库
        knowledgeRepository.saveAll(knowledgeDocs);
        System.out.println("✅ 成功入库 " + knowledgeDocs.size() + " 个片段: " + sourceFilename);
    }

    // [新增] 级联删除：根据 fileId 删除所有的向量片段
    public void deleteByFileId(String fileId) {
        // 这里需要去 Repository 加一个方法，或者用 MongoTemplate
        // 简单起见，我们先去 KnowledgeRepository 加一个 deleteByFileId
        knowledgeRepository.deleteByFileId(fileId);
    }
}