package com.example.knittdaserver.dto;

import com.example.knittdaserver.entity.ProjectStatus;
import com.example.knittdaserver.entity.RecordStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class CreateRecordRequest {

    @NotNull(message = "프로젝트 ID는 필수입니다.")
    private Long projectId;

    @NotNull(message = "진행도는 필수입니다.")
    private RecordStatus recordStatus;

    private List<String> tags;

    @NotBlank(message = "기록 내용은 필수입니다.")
    private String comment;

    private LocalDateTime recordedAt;
}