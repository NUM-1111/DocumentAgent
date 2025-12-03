package com.intellivault.backend.controller;

import com.intellivault.backend.event.DocumentUploadEvent;
import com.intellivault.backend.service.DocumentService; // [1] 确保导入
import com.intellivault.backend.service.DocumentStorageService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentStorageService storageService;
    private final ApplicationEventPublisher publisher;
    private final DocumentService documentService; // [2] 加回这个字段

    // [3] 构造器注入 DocumentService，以便 delete 方法使用
    public DocumentController(DocumentStorageService storageService,
                              ApplicationEventPublisher publisher,
                              DocumentService documentService) {
        this.storageService = storageService;
        this.publisher = publisher;
        this.documentService = documentService;
    }

    /**
     * 1. 上传接口 (异步)
     */
    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("文件不能为空");

        try {
            // 1. IO 操作：存 GridFS
            String fileId = storageService.storeFile(file);

            // 2. 发布事件 (由 Listener 异步调用 DocumentService 进行解析和入库)
            publisher.publishEvent(new DocumentUploadEvent(this, fileId, file.getOriginalFilename(), "user_001"));

            return ResponseEntity.accepted().body(Map.of(
                    "status", "processing",
                    "fileId", fileId,
                    "message", "文档已进入后台处理队列"
            ));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("上传失败: " + e.getMessage());
        }
    }

    /**
     * 2. 下载接口
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<?> download(@PathVariable String id) {
        return storageService.getFile(id)
                .map(gridFsResource -> {
                    try {
                        String filename = URLEncoder.encode(gridFsResource.getFilename(), StandardCharsets.UTF_8);
                        return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(gridFsResource.getContentType()))
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                                .body((Resource) gridFsResource);
                    } catch (Exception e) {
                        return ResponseEntity.internalServerError().build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 3. 删除接口 (同步)
     * 因为删除操作很快，不需要异步，直接调用 DocumentService 即可
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        // 1. 删原文件
        storageService.deleteFile(id);

        // 2. 删向量数据 (这里现在可以正常调用了)
        documentService.deleteByFileId(id);

        return ResponseEntity.ok(Map.of("status", "deleted", "id", id));
    }
}