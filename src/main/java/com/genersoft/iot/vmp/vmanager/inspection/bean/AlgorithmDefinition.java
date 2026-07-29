package com.genersoft.iot.vmp.vmanager.inspection.bean;

import lombok.Data;

@Data
public class AlgorithmDefinition {
    private Integer id;
    private String code;
    private String name;
    private String category;
    private Integer minDurationSeconds;
    private Integer cooldownSeconds;
    private Double confidenceThreshold;
    private String riskLevel;
    private Boolean enabled;
}
