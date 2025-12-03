package com.intellivault.backend.service;

import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

@Service
public class FileParseService {

    // 1. 保持原有接口，适配 Controller 的直接调用（如果有的话）
    public String parse(MultipartFile file) {
        try {
            return parse(new InputStreamResource(file.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException("文件流读取失败", e);
        }
    }

    // 2. [新增] 核心逻辑下沉，支持通用 Resource (适配 GridFSResource)
    public String parse(Resource resource) {
        try {
            // TikaDocumentReader 核心逻辑
            TikaDocumentReader reader = new TikaDocumentReader(resource);
            return reader.get().stream()
                    .map(doc -> doc.getContent())
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new RuntimeException("文档解析内部错误", e);
        }
    }
}