package com.genersoft.iot.vmp.vmanager.message.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "站内消息")
public class SystemMessage {
    private Integer id;
    private Integer userId;
    private String type;
    private String title;
    private String content;
    private String level;
    private String businessType;
    private Integer businessId;
    private Boolean readFlag;
    private String readTime;
    private String createTime;
    private String alarmStatus;
    private String deviceId;
    private String channelId;
}
