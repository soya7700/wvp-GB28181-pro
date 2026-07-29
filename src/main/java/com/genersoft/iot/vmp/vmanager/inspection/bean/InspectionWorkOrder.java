package com.genersoft.iot.vmp.vmanager.inspection.bean;

import lombok.Data;

@Data
public class InspectionWorkOrder {
    private Long id;
    private Long resultId;
    private String title;
    private String priority;
    private String status;
    private Integer assigneeId;
    private String dueTime;
    private String acceptedAt;
    private String resolvedAt;
    private String verifiedAt;
    private String resolution;
    private String createTime;
    private String updateTime;
}
