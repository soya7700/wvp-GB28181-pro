package com.genersoft.iot.vmp.vmanager.inspection.service;

import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionPlan;
import com.genersoft.iot.vmp.vmanager.inspection.bean.InspectionTask;
import com.genersoft.iot.vmp.vmanager.inspection.conf.InspectionProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AiInspectionClient {
    private final InspectionProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public AiInspectionClient(InspectionProperties properties) {
        this.properties = properties;
    }

    public boolean configured() {
        return properties.isEnabled() && properties.getServiceUrl() != null
                && !properties.getServiceUrl().trim().isEmpty();
    }

    public void dispatch(InspectionPlan plan, InspectionTask task) {
        if (!configured()) return;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("taskId", task.getId());
        body.put("planId", plan.getId());
        body.put("channelIds", plan.getChannelIds());
        body.put("detectionTypes", plan.getDetectionTypes());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!properties.getCallbackToken().isEmpty()) {
            headers.set("X-AI-Callback-Token", properties.getCallbackToken());
        }
        restTemplate.postForEntity(normalize(properties.getServiceUrl()) + "/tasks",
                new HttpEntity<>(body, headers), Void.class);
    }

    private String normalize(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
