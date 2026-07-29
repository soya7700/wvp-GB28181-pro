package com.genersoft.iot.vmp.vmanager.inspection.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Schema(description = "AI巡检能力概览")
public class InspectionOverview {

    private boolean enabled;
    private String serviceStatus;
    private String serviceUrl;
    private String phase;
    private List<Capability> capabilities;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Capability {
        private String code;
        private String name;
        private String status;
        private String description;
    }
}
