package com.genersoft.iot.vmp.vmanager.inspection.bean;

import lombok.Data;

@Data
public class SceneTemplate {
    private Integer id;
    private String code;
    private String name;
    private String description;
    private String status;
    private Integer version;
    private String createTime;
    private String updateTime;
}
