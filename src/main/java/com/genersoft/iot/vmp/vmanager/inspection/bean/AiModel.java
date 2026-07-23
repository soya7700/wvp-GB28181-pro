package com.genersoft.iot.vmp.vmanager.inspection.bean;

import lombok.Data;

@Data
public class AiModel {
    private Integer id;
    private String name;
    private String version;
    private String capabilities;
    private String status;
    private String serviceEndpoint;
    private Integer trafficPercent;
    private String createTime;
}
