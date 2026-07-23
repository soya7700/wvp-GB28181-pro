package com.genersoft.iot.vmp.vmanager.inspection.bean;
import lombok.Data;
@Data
public class MobileRecorder {
    private Long id;
    private String deviceCode;
    private String name;
    private String vendor;
    private String model;
    private String protocolType;
    private String simNumber;
    private String organizationId;
    private Integer assignedUserId;
    private String status;
    private Integer batteryLevel;
    private Integer storagePercent;
    private String networkStatus;
    private String capabilities;
    private String lastOnlineTime;
    private String createTime;
    private String updateTime;
}
