package com.example.knittdaserver.dto;

import com.example.knittdaserver.entity.Design;
import com.example.knittdaserver.entity.ProjectStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class UpdateProjectRequest {
    @NotNull(message = "프로젝트 ID는 필수 입니다.")
    private Long projectId;
    private String nickname;
    private ProjectStatus status;

    private String designTitle;
    private String designer;
    private String yarnInfo;
    private String needleInfo;
    private String description;
    private Boolean visible;

    // 기본 이미지로 변경 시 전달 (file 미첨부 시에만 사용). null 이면 썸네일 변경 없음.
    private Long defaultThumbnailId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate goalDate;
}
