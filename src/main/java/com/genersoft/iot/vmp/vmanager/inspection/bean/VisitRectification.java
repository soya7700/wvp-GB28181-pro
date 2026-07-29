package com.genersoft.iot.vmp.vmanager.inspection.bean;
import lombok.Data;
@Data
public class VisitRectification {
    private Long id;
    private Long taskId;
    private Long checkResultId;
    private String storeId;
    private String storeName;
    private String title;
    private String severity;
    private Integer assigneeId;
    private String status;
    private String dueTime;
    private String resolution;
    private String evidenceUrls;
    private String submittedAt;
    private String reviewedAt;
    private Integer reviewedBy;
    private String reviewNote;
    private String createTime;
    private String updateTime;
}
