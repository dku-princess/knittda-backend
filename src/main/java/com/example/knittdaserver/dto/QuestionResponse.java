package com.example.knittdaserver.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {
    private Long projectId;
    private String projectName;
    private String designTitle;
    private String projectStatus;
    private List<RecordDto> records;
    private Map<Integer, String> questions; // 버전별 질문 (1~8)
} 