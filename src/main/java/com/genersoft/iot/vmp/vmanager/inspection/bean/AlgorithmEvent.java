package com.genersoft.iot.vmp.vmanager.inspection.bean;

import lombok.Data;

@Data
public class AlgorithmEvent {
    private Long id;
    private String eventUid;
    private Integer templateId;
    private Integer regionId;
    private String algorithmCode;
    private String algorithmVersion;
    private String deviceId;
    private String channelId;
    private String targetId;
    private Double confidence;
    private String startTime;
    private String endTime;
    private Integer durationSeconds;
    private String state;
    private String evidenceUrl;
    private String clipUrl;
    private String dedupKey;
    private String recoveredAt;
    private String suppressedUntil;
    private Integer occurrenceCount;
    private String reviewStatus;
    private Integer reviewedBy;
    private String reviewedAt;
    private String reviewNote;
    private String createTime;
}
