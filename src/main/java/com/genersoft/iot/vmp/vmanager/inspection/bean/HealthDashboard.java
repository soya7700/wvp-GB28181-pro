package com.genersoft.iot.vmp.vmanager.inspection.bean;

import lombok.Data;
import java.util.List;

@Data
public class HealthDashboard {
    private int total;
    private int healthy;
    private int warning;
    private int critical;
    private double averageScore;
    private List<ChannelHealth> problemChannels;
}
