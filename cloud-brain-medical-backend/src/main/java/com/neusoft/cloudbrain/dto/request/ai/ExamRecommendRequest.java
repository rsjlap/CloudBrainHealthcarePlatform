package com.neusoft.cloudbrain.dto.request.ai;

import lombok.Data;

@Data
public class ExamRecommendRequest {

    private String chiefComplaint;
    private String symptoms;
    private String diagnosis;
}
