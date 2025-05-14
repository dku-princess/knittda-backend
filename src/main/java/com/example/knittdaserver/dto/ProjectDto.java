package com.example.knittdaserver.dto;

import com.example.knittdaserver.entity.Image;
import com.example.knittdaserver.entity.ImageDto;
import com.example.knittdaserver.entity.Project;
import com.example.knittdaserver.entity.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
@Builder
@ToString
public class ProjectDto {
    private Long id;
    private Long designId;
    private Long userId;
    private String nickname;
    private ProjectStatus status; // enum
    private String customYarnInfo;
    private String customNeedleInfo;
    private LocalDateTime lastRecordAt;
    private LocalDateTime createdAt;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate goalDate;
    private ImageDto image;

    public static ProjectDto from(Project project) {
        return ProjectDto.builder()
                .id(project.getId())
                .designId(project.getDesign().getId())
                .userId(project.getUser().getId())
                .nickname(project.getNickname())
                .status(project.getStatus())
                .customYarnInfo(project.getCustomYarnInfo())
                .customNeedleInfo(project.getCustomNeedleInfo())
                .lastRecordAt(project.getLastRecordAt())
                .createdAt(project.getCreatedAt())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .goalDate(project.getGoalDate())
                .image(ImageDto.from(project.getImage()))
                .build();
    }


}