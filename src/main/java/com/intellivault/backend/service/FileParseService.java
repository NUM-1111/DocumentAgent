package com.intellivault.backend.service;

import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

@Service
public class FileParseService {

    /**
     * 解析各类文档 (PDF, Word, MD, etc.) 为纯文本
     * * @param file 用户上传的文件
     * @return 解析后的纯文本内容
     */
    public String parse(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            // 1. 将 MultipartFile 转换为 Spring Resource
            // TikaDocumentReader 需要 Resource 接口作为输入
            var resource = new InputStreamResource(inputStream);

            // 2. 创建 Tika 读取器
            // Spring AI 封装了 Apache Tika，能自动识别 MIME Type
            TikaDocumentReader reader = new TikaDocumentReader(resource);

            // 3. 提取文本
            // get() 返回的是 List<Document>，我们这里合并成一个长字符串
            // 面试点：对于超长 PDF，后续会在 DocumentService 里做 Split，这里先拿全量文本
            return reader.get().stream()
                    .map(doc -> doc.getContent())
                    .collect(Collectors.joining("\n"));

        } catch (IOException e) {
            throw new RuntimeException("文档解析失败: " + file.getOriginalFilename(), e);
        }
    }
}