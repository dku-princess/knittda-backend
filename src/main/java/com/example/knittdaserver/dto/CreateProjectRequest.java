package com.example.knittdaserver.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectRequest {

    @NotBlank(message = "프로젝트 이름은 필수입니다.")
    private String nickname;

    // 도안 관련 정보
    private String designTitle;
    private String designer;
    private String yarnInfo;
    private String needleInfo;
    private String description;

    // 프로젝트 공개 여부
    private Boolean visible;

    // 기본 이미지 선택 시 전달 (file 미첨부 시에만 사용). null 이면 기본 이미지 미선택.
    private Long defaultThumbnailId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate goalDate;


}
