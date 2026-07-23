package com.genersoft.iot.vmp.vmanager.inspection.bean;
import lombok.Data;
@Data
public class VisitMediaFile {
    private Long id;
    private Long taskId;
    private Long recorderId;
    private String uploadId;
    private String mediaType;
    private String fileName;
    private Long fileSize;
    private String checksum;
    private String storageUrl;
    private String thumbnailUrl;
    private String status;
    private Integer uploadedBytes;
    private String capturedAt;
    private Integer uploadedBy;
    private String createTime;
    private String updateTime;
}
