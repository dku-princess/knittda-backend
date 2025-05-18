package com.example.knittdaserver.dto;

import com.example.knittdaserver.entity.ImageDto;
import com.example.knittdaserver.entity.Project;
import com.example.knittdaserver.entity.ProjectStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
@ToString
public class ProjectDto {

    private Long id;
    private DesignDto designDto;
    private Long userId;
    private String nickname;
    private ProjectStatus status;
    private String customYarnInfo;
    private String customNeedleInfo;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime lastRecordAt;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime createdAt;

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate goalDate;

    private ImageDto image;

    public static ProjectDto from(Project project) {
        return ProjectDto.builder()
                .id(project.getId())
                .designDto(DesignDto.from(project.getDesign()))
                .userId(project.getUser().getId())
                .nickname(project.getNickname())
                .status(project.getStatus())
                .customYarnInfo(project.getCustomYarnInfo())
                .customNeedleInfo(project.getCustomNeedleInfo())
                .createdAt(project.getCreatedAt())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .goalDate(project.getGoalDate())
                .image(ImageDto.from(project.getImage()))
                .build();
    }
}
