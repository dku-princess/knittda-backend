package com.example.knittdaserver.dto;

import com.example.knittdaserver.entity.Project;
import com.example.knittdaserver.entity.RecordStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Builder
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class UpdateRecordRequest {

    @NotNull(message = "기록 ID는 필수입니다.")
    private Long recordId;
    private Project project;
    private RecordStatus recordStatus;
    private List<String> tags;
    private String comment;
}
