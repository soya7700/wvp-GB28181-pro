package com.genersoft.iot.vmp.vmanager.inspection.bean;
import lombok.Data;
@Data
public class SceneRiskSummary {
    private Integer templateId;
    private String templateName;
    private Integer eventCount;
    private Integer openCount;
    private Integer highRiskCount;
    private Integer confirmedCount;
    private Integer falsePositiveCount;
    private Integer riskScore;
    private String riskLevel;
}
