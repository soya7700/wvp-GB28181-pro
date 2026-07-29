package com.genersoft.iot.vmp.vmanager.inspection.bean;

import lombok.Data;

@Data
public class AiRule {
    private Integer id;
    private String name;
    private Integer planId;
    private String detectionType;
    private Double confidenceThreshold = 0.8;
    private String regionPoints;
    private boolean enabled = true;
    private String createTime;
    private String updateTime;
}
