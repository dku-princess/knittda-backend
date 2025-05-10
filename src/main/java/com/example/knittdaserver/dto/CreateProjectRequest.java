package com.example.knittdaserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Builder
public class CreateProjectRequest {

    @NotNull(message = "디자인 ID는 필수입니다.")
    private Long designId;

    @NotBlank(message = "프로젝트 이름은 필수입니다.")
    private String nickname;

    private String customYarnInfo;
    private String customNeedleInfo;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate goalDate;

}
