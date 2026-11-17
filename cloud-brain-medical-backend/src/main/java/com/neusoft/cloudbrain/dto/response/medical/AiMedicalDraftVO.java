package com.neusoft.cloudbrain.dto.response.medical;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiMedicalDraftVO {

    private String chiefComplaint;
    private String presentIllness;
    private String pastHistory;
    private String allergyHistory;
    private String personalHistory;
    private String maritalHistory;
    private String familyHistory;
    private String temperature;
    private String respiration;
    private String pulse;
    private String bloodPressure;
    private String physicalExam;
    private String auxiliaryExam;
    private String diagnosis;
    private String treatmentPlan;
    private List<RecommendedExamVO> recommendedExams;
    private boolean aiGenerated;
    private boolean aiAvailable;
    private boolean kgAvailable;
    private String kgReference;
}
