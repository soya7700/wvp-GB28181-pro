package com.genersoft.iot.vmp.vmanager.inspection.bean;
import lombok.Data;
@Data
public class VisitOperationsSummary {
    private Integer taskCount;
    private Integer completedCount;
    private Integer storeCount;
    private Integer problemCount;
    private Integer rectificationCount;
    private Integer closedCount;
    private Integer overdueCount;
    private Double completionRate;
    private Double rectificationRate;
}
