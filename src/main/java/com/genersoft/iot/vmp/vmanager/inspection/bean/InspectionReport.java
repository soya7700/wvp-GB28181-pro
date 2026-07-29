package com.genersoft.iot.vmp.vmanager.inspection.bean;

import lombok.Data;

@Data
public class InspectionReport {
    private int taskCount;
    private int completedCount;
    private int abnormalCount;
    private int pendingCount;
    private int confirmedCount;
    private int falsePositiveCount;
}
