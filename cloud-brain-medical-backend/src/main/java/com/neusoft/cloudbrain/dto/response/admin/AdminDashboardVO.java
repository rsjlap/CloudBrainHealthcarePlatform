package com.neusoft.cloudbrain.dto.response.admin;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminDashboardVO {

    private long todayRegistrations;
    private long totalRegistrations;
    private long inProgressCount;
    private long completedCount;
    private long patientCount;
    private long doctorCount;
    private List<ChartCountItemVO> registrationTrend;
    private List<ChartCountItemVO> departmentDistribution;
}
