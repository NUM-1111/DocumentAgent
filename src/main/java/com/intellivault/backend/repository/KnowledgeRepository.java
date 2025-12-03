package com.intellivault.backend.repository;

import com.intellivault.backend.model.KnowledgeDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeRepository extends MongoRepository<KnowledgeDocument, String> {

    // 基础查询，后续我们会用 MongoTemplate 做复杂的向量查询
    List<KnowledgeDocument> findBySourceFilename(String sourceFilename);
    // [新增] 用于级联删除
    void deleteByFileId(String fileId);
}