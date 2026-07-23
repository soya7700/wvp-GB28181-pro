package com.genersoft.iot.vmp.vmanager.inspection.bean;

import lombok.Data;

@Data
public class InspectionPlan {
    private Integer id;
    private String name;
    private boolean enabled = true;
    private Integer intervalMinutes = 30;
    private String detectionTypes;
    private String channelIds;
    private String createTime;
    private String updateTime;
}
