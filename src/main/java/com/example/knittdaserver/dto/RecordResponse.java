package com.example.knittdaserver.dto;

import com.example.knittdaserver.entity.Record;
import com.example.knittdaserver.entity.RecordStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@ToString
public class RecordResponse {
    private Long id;
    private ProjectDto projectDto;
    private RecordStatus recordStatus;
    private List<String> tags;
    private String comment;
    private LocalDateTime createdAt;

    public static RecordResponse from(Record record) {
        return RecordResponse.builder()
                .id(record.getId())
                .projectDto(ProjectDto.from(record.getProject()))
                .recordStatus(record.getRecordStatus())
                .tags(record.getTags())
                .comment(record.getComment())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
