package com.intellivault.backend.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

//简单的 POJO 来承载事件数据
@Getter
public class DocumentUploadEvent extends ApplicationEvent {

    private final String fileId;
    private final String fileName;
    private final String userId; // 预留，方便后续扩展

    public DocumentUploadEvent(Object source, String fileId, String fileName, String userId) {
        super(source);
        this.fileId = fileId;
        this.fileName = fileName;
        this.userId = userId;
    }
}