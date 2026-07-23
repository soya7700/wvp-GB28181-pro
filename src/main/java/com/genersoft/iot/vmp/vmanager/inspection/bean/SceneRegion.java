package com.genersoft.iot.vmp.vmanager.inspection.bean;

import lombok.Data;

@Data
public class SceneRegion {
    private Integer id;
    private Integer templateId;
    private String name;
    private String regionType;
    private String polygonPoints;
    private String excludedPoints;
    private String channelIds;
    private String algorithmCodes;
    private String activeDays;
    private String startTime;
    private String endTime;
    private Boolean enabled;
}
