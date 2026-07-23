package com.genersoft.iot.vmp.vmanager.inspection.bean;
import lombok.Data;
@Data
public class MaintenanceWindow {
    private Long id;
    private String scopeType;
    private String scopeId;
    private String startTime;
    private String endTime;
    private String reason;
    private Boolean enabled;
    private String createTime;
}
