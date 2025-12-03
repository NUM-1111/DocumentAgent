package com.intellivault.backend.controller;

import com.intellivault.backend.service.DocumentService;
import com.intellivault.backend.service.DocumentStorageService;
import com.intellivault.backend.service.FileParseService;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
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
    private final FileParseService fileParseService;
    private final DocumentService documentService;

    public DocumentController(DocumentStorageService storageService,
                              FileParseService fileParseService,
                              DocumentService documentService) {
        this.storageService = storageService;
        this.fileParseService = fileParseService;
        this.documentService = documentService;
    }

    /**
     * 1. 上传接口
     * 流程：存 GridFS -> 解析文本 -> 向量化入库
     */
    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("文件不能为空");

        try {
            // Step 1: 存原文件 (GridFS)
            String fileId = storageService.storeFile(file);

            // Step 2: 解析文本 (Tika)
            String content = fileParseService.parse(file);

            // Step 3: 向量化 (Vector DB)
            documentService.processAndStore(content, file.getOriginalFilename(), fileId);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "fileId", fileId,
                    "fileName", file.getOriginalFilename()
            ));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("上传失败: " + e.getMessage());
        }
    }

    /**
     * 2. 下载接口
     * 修改点：返回类型由 ResponseEntity<Resource> 改为 ResponseEntity<?>
     * 这样既可以返回 Resource (成功)，也可以返回 Void (404/500 错误)
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<?> download(@PathVariable String id) { // <--- 这里改了
        return storageService.getFile(id)
                .map(gridFsResource -> {
                    try {
                        // 处理文件名中文乱码问题
                        String filename = URLEncoder.encode(gridFsResource.getFilename(), StandardCharsets.UTF_8);
                        return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(gridFsResource.getContentType()))
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                                .body((Resource) gridFsResource);
                    } catch (Exception e) {
                        // 发生异常时返回 500
                        return ResponseEntity.internalServerError().build();
                    }
                })
                // 文件不存在时返回 404
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 3. 删除接口
     * 级联删除：既删 GridFS 的文件，也删向量库里的数据
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        // 1. 删原文件
        storageService.deleteFile(id);

        // 2. 删向量数据
        documentService.deleteByFileId(id);

        return ResponseEntity.ok(Map.of("status", "deleted", "id", id));
    }

    // TODO: 还需要一个 list 接口，但目前 KnowledgeDocument 是按片段存的。
    // 如果要列出“有哪些文件”，建议单独建一个 Collection 存文件元数据 (FileMetadata)，
    // 或者直接查询 GridFS 的 fs.files 集合。这里先略过，保证核心功能可用。
}