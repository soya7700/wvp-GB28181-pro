package com.genersoft.iot.vmp.vmanager.inspection;

import com.genersoft.iot.vmp.vmanager.inspection.dao.InspectionMapper;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InspectionMapperSqlTest {

    @Test
    void reportQueriesUseSqlComparisonOperator() {
        assertComparisonOperator("taskCount");
        assertComparisonOperator("completedCount");
        assertComparisonOperator("abnormalCount");
        assertComparisonOperator("resultCount");
    }

    @Test
    void dailyMetricsShouldNotUseCorrelatedUngroupedTaskTime() {
        Method method = Arrays.stream(InspectionMapper.class.getMethods())
                .filter(item -> item.getName().equals("dailyMetrics"))
                .findFirst()
                .orElseThrow(AssertionError::new);
        String sql = String.join(" ", method.getAnnotation(Select.class).value());

        assertFalse(sql.contains("DATE(t.start_time)"),
                "dailyMetrics must not correlate a non-grouped task timestamp");
        assertTrue(sql.contains("task_daily"));
        assertTrue(sql.contains("result_daily"));
    }

    private void assertComparisonOperator(String methodName) {
        Method method = Arrays.stream(InspectionMapper.class.getMethods())
                .filter(item -> item.getName().equals(methodName))
                .findFirst()
                .orElseThrow(AssertionError::new);
        String sql = String.join(" ", method.getAnnotation(Select.class).value());
        assertTrue(sql.contains(">="), methodName + " should use a SQL comparison operator");
        assertFalse(sql.contains("&gt;"), methodName + " should not contain an XML entity");
    }
}
