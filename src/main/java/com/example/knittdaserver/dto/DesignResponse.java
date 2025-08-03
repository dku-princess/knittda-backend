package com.example.knittdaserver.dto;

import java.time.LocalDateTime;

import com.example.knittdaserver.entity.Design;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DesignResponse {
    private Long id;
    private String title;
    private String designer;
    private String needleInfo;
    private String yarnInfo;
    private String description;
    private LocalDateTime createdAt;

    public static DesignResponse from(Design design) {
        return DesignResponse.builder()
                .id(design.getId())
                .title(design.getTitle())
                .designer(design.getDesigner())
                .needleInfo(design.getNeedleInfo())
                .yarnInfo(design.getYarnInfo())
                .description(design.getDescription())
                .createdAt(design.getCreatedAt())
                .build();
    }
}
