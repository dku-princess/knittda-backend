package com.example.knittdaserver.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Builder
public class CreateProjectRequest {

    @NotBlank(message = "프로젝트 이름은 필수입니다.")
    private String nickname;
    private Long designId;
    private String customYarnInfo;
    private String customNeedleInfo;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate goalDate;
    private DesignInfo designInfo;

    @Data
    public static class DesignInfo {
        private String title;
        private String designer;
        private boolean isOfficial;
    }

}
