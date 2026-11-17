package com.neusoft.cloudbrain.dto.response.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.neusoft.cloudbrain.dto.response.medical.RecommendedExamVO;
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
public class ExamRecommendVO {

    private List<RecommendedExamVO> recommendedExams;
    private String summary;
    private boolean aiAvailable;
    private boolean kgAvailable;
    private String kgReference;
}
