package com.example.knittdaserver.dto;

import com.example.knittdaserver.entity.Record;
import com.example.knittdaserver.entity.RecordStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class RecordDto {
    private Long id;
    private Long projectId;
    private RecordStatus recordStatus;
    private List<String> tags;
    private String comment;
    private LocalDateTime createdAt;

    public static RecordDto from(Record record) {
        return RecordDto.builder()
                .id(record.getId())
                .projectId(record.getProject().getId())
                .recordStatus(record.getRecordStatus())
                .tags(record.getTags())
                .comment(record.getComment())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
