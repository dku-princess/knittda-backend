package com.example.knittdaserver.dto;

import com.example.knittdaserver.entity.ProjectStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class UpdateProjectRequest {
    @NotNull(message = "프로젝트 ID는 필수 입니다.")
    private Long projectId;
    private String nickname;
    private ProjectStatus status;
    private String customYarnInfo;
    private String customNeedleInfo;
    private LocalDateTime lastRecordAt;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate goalDate;
}
