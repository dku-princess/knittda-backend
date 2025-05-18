package com.example.knittdaserver.dto;

import com.example.knittdaserver.entity.ProjectStatus;
import com.example.knittdaserver.entity.RecordStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateRecordRequest {

    @NotNull(message = "프로젝트 ID는 필수입니다.")
    private Long projectId;

    @NotNull(message = "진행도는 필수입니다.")
    private RecordStatus recordStatus;

    private List<String> tags;

    @NotBlank(message = "기록 내용은 필수입니다.")
    private String comment;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime recordedAt;
}