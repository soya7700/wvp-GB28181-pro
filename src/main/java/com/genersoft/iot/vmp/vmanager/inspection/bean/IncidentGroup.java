package com.genersoft.iot.vmp.vmanager.inspection.bean;

import lombok.Data;

@Data
public class IncidentGroup {
    private String aggregationKey;
    private String rootCause;
    private Integer eventCount;
    private Integer affectedChannels;
    private String priority;
    private String latestTime;
}
