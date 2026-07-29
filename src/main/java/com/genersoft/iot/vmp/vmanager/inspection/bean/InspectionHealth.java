package com.genersoft.iot.vmp.vmanager.inspection.bean;

import lombok.Data;

@Data
public class InspectionHealth {
    private boolean migrationReady;
    private int tableCount;
    private boolean aiConfigured;
    private String serviceUrl;
    private String status;
}
