package com.genersoft.iot.vmp.vmanager.inspection.bean;

import lombok.Data;

@Data
public class InspectionTask {
    private Long id;
    private Integer planId;
    private String planName;
    private String status;
    private Integer channelTotal;
    private Integer successCount;
    private Integer abnormalCount;
    private String startTime;
    private String endTime;
    private String errorMessage;
    private Integer retryCount;
}
