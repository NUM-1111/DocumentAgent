package com.intellivault.backend.event;

import com.intellivault.backend.service.DocumentService;
import com.intellivault.backend.service.DocumentStorageService;
import com.intellivault.backend.service.FileParseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.core.io.Resource;

@Slf4j // 需要 Lombok 支持日志
@Component
public class DocumentUploadListener {

    private final DocumentStorageService storageService;
    private final FileParseService parseService;
    private final DocumentService documentService;

    public DocumentUploadListener(DocumentStorageService storageService,
                                  FileParseService parseService,
                                  DocumentService documentService) {
        this.storageService = storageService;
        this.parseService = parseService;
        this.documentService = documentService;
    }

    @Async // [核心] 让这个方法在独立线程池中运行
    @EventListener
    public void handleUploadEvent(DocumentUploadEvent event) {
        String fileId = event.getFileId();
        String fileName = event.getFileName();

        log.info("⚡ [异步任务] 开始处理文档: ID={}, Name={}", fileId, fileName);

        try {
            // 1. 从 GridFS 捞回文件流
            // 注意：storageService.getFile 返回的是 Optional<GridFsResource>
            Resource resource = storageService.getFile(fileId)
                    .orElseThrow(() -> new RuntimeException("GridFS 中找不到文件: " + fileId));

            // 2. 解析文本 (Tika) - 耗时操作
            String content = parseService.parse(resource);
            log.info("📄 文档解析完成，长度: {}", content.length());

            // 3. 向量化并入库 (Embedding) - 耗时操作
            documentService.processAndStore(content, fileName, fileId);

            log.info("✅ [异步任务] 文档处理成功结束: {}", fileName);

        } catch (Exception e) {
            // 生产环境这里应该写入“任务失败表”，供后续重试
            log.error("❌ [异步任务] 处理失败: {}", e.getMessage(), e);
        }
    }
}