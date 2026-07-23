package com.genersoft.iot.vmp.vmanager.inspection.bean;
import lombok.Data;
@Data
public class VisitChecklistResult {
    private Long id;
    private Long taskId;
    private String submissionId;
    private String itemCode;
    private String itemName;
    private String itemGroup;
    private String result;
    private String severity;
    private String note;
    private String evidenceUrls;
    private Boolean evidenceRequired;
    private Integer submittedBy;
    private String submittedAt;
}
