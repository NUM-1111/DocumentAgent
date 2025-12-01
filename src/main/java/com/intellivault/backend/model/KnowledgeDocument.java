package com.intellivault.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "knowledge_docs")
public class KnowledgeDocument {

    @Id
    private String id;

    // 原始文本内容 (用于后续传给大模型)
    private String content;

    // 向量数据 (High-Dimensional Vector)
    // 简历亮点：这里我们选择手动管理向量，为后续自定义检索算法预留空间
    private List<Double> embedding;

    // 元数据 (来源、作者、上传时间等)
    private Map<String, Object> metadata;

    // 加上索引，虽然 MVP 阶段我们可能暂时用不到 Mongo 的 Atlas Search，
    // 但这个注解展示了你对数据库性能的关注
    @Indexed
    private String sourceFilename;
}