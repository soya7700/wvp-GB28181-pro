package com.genersoft.iot.vmp.vmanager.inspection.bean;
import lombok.Data;
@Data
public class StoreVisitTask {
    private Long id;
    private String taskCode;
    private String title;
    private String storeId;
    private String storeName;
    private Double storeLongitude;
    private Double storeLatitude;
    private Integer assigneeId;
    private Long recorderId;
    private String plannedStartTime;
    private String plannedEndTime;
    private String status;
    private String checkedInAt;
    private String checkedOutAt;
    private Double checkinDistanceMeters;
    private String createTime;
    private String updateTime;
}
