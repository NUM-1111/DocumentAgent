package com.intellivault.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "knowledge_docs")
public class KnowledgeDocument {

    @Id
    private String id;

    // [新增] 关联 MongoDB GridFS 中的原始文件 ID
    // 这样前端展示搜索结果时，可以提供“下载原文件”的链接
    @Indexed
    private String fileId;

    private String content;

    private List<Double> embedding;

    private Map<String, Object> metadata;

    @Indexed
    private String sourceFilename;
}