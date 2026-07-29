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
    private String scheduleDays = "1,2,3,4,5,6,7";
    private String startTime = "00:00";
    private String endTime = "23:59";
    private String createTime;
    private String updateTime;
}
