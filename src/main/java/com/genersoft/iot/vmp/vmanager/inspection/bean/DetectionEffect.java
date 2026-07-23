package com.genersoft.iot.vmp.vmanager.inspection.bean;

import lombok.Data;

@Data
public class DetectionEffect {
    private String detectionType;
    private Integer totalCount;
    private Integer confirmedCount;
    private Integer falsePositiveCount;

    public double getConfirmationRate() {
        return totalCount == null || totalCount == 0 ? 0 : (double) confirmedCount / totalCount;
    }
}
