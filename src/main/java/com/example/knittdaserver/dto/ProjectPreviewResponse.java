package com.example.knittdaserver.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectPreviewResponse {
    private Long projectId;
    private String userName;
    private String projectName;
    private int recordNum;
    private LocalDateTime lastRecordAt;
    private List<String> recentImageUrls;
}
