package com.genersoft.iot.vmp.vmanager.inspection.bean;
import lombok.Data;
@Data
public class RecorderLocation {
    private Long id;
    private Long recorderId;
    private Double longitude;
    private Double latitude;
    private String coordinateType;
    private Integer accuracyMeters;
    private String locateTime;
}
