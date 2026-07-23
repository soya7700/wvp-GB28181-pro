package com.genersoft.iot.vmp.vmanager.inspection.bean;

import lombok.Data;

@Data
public class InspectionResult {
    private Long id;
    private String callbackId;
    private Long taskId;
    private String deviceId;
    private String channelId;
    private String detectionType;
    private Double confidence;
    private String status;
    private String workflowStatus;
    private String priority;
    private Integer assigneeId;
    private Integer occurrenceCount;
    private String handlingNote;
    private String handledAt;
    private String aggregationKey;
    private String rootCause;
    private String recoveredAt;
    private Integer modelId;
    private Integer ruleId;
    private String evidenceUrl;
    private String markedUrl;
    private String createTime;
    private String reviewNote;
    private Integer reviewedBy;
    private String reviewedAt;
    private Integer alarmId;
}
