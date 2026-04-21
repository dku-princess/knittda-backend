package com.example.knittdaserver.dto;

import com.example.knittdaserver.entity.Record;
import com.example.knittdaserver.entity.RecordStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RecordResponse {
    private Long id;
    private Long projectId;
    private RecordStatus recordStatus;
    private List<String> tags;
    private String comment;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime createdAt;

    private List<ImageDto> images;

    public static RecordResponse from(Record record) {
        return RecordResponse.builder()
                .id(record.getId())
                .projectId(record.getProject().getId()) 
                .recordStatus(record.getRecordStatus())
                .tags(record.getTags() != null ? record.getTags() : new ArrayList<>())
                .comment(record.getComment())
                .createdAt(record.getCreatedAt())
                .images(
                        record.getImages().stream()
                                .map(ImageDto::from)
                                .collect(Collectors.toList())
                )
                .build();
    }
}
