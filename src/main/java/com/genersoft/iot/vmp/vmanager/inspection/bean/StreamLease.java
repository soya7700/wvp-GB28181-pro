package com.genersoft.iot.vmp.vmanager.inspection.bean;
import lombok.Data;
@Data
public class StreamLease {
    private Long id;
    private String tenantId;
    private Long recorderId;
    private String businessType;
    private String leaseToken;
    private String status;
    private String expiresAt;
    private String createTime;
}
