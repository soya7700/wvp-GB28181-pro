package com.genersoft.iot.vmp.vmanager.inspection.conf;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai.inspection")
public class InspectionProperties {
    private boolean enabled;
    private String serviceUrl = "";
    private String callbackToken = "";
    private int connectTimeoutMillis = 3000;
    private int readTimeoutMillis = 10000;
    private int maxRetries = 2;
    private long retryDelayMillis = 500;
    private long schedulerLockSeconds = 120;
    private long callbackMaxSkewSeconds = 300;
    private long callbackNonceSeconds = 600;
}
