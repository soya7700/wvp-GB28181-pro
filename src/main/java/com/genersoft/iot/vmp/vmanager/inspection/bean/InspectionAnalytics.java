package com.genersoft.iot.vmp.vmanager.inspection.bean;

import lombok.Data;
import java.util.List;

@Data
public class InspectionAnalytics {
    private List<DailyMetric> daily;
    private List<ChannelMetric> topChannels;

    @Data
    public static class DailyMetric {
        private String day;
        private Integer taskCount;
        private Integer abnormalCount;
        private Integer confirmedCount;
    }

    @Data
    public static class ChannelMetric {
        private String channelId;
        private Integer abnormalCount;
        private Integer confirmedCount;
    }
}
