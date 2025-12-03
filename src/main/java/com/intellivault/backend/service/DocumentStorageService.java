package com.intellivault.backend.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Service
public class DocumentStorageService {

    private final GridFsTemplate gridFsTemplate;

    public DocumentStorageService(GridFsTemplate gridFsTemplate) {
        this.gridFsTemplate = gridFsTemplate;
    }

    /**
     * 1. 存储文件到 GridFS
     */
    public String storeFile(MultipartFile file) throws IOException {
        ObjectId fileId = gridFsTemplate.store(
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType()
        );
        return fileId.toString();
    }

    /**
     * 2. 获取文件资源 (用于下载)
     */
    public Optional<GridFsResource> getFile(String id) {
        GridFSFile gridFSFile = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(id)));
        if (gridFSFile == null) {
            return Optional.empty();
        }
        return Optional.of(gridFsTemplate.getResource(gridFSFile));
    }

    /**
     * 3. 删除文件
     */
    public void deleteFile(String id) {
        gridFsTemplate.delete(new Query(Criteria.where("_id").is(id)));
    }
}