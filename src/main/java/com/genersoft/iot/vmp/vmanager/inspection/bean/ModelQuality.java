package com.genersoft.iot.vmp.vmanager.inspection.bean;

import lombok.Data;

@Data
public class ModelQuality {
    private Integer modelId;
    private String modelName;
    private String modelVersion;
    private Integer totalCount;
    private Integer confirmedCount;
    private Integer falsePositiveCount;
    private double confirmationRate;
    private double falsePositiveRate;
    private String qualityStatus;
}
