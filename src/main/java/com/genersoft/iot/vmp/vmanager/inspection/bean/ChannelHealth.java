package com.genersoft.iot.vmp.vmanager.inspection.bean;

import lombok.Data;

@Data
public class ChannelHealth {
    private Long id;
    private String deviceId;
    private String channelId;
    private Boolean online;
    private Boolean streamAvailable;
    private Integer firstFrameMillis;
    private Integer videoQualityScore;
    private Boolean recordingComplete;
    private Integer healthScore;
    private String healthStatus;
    private String diagnostic;
    private String snapshotUrl;
    private String checkTime;
}
