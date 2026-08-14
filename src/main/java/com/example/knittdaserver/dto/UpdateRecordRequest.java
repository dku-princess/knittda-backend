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
    private String recordStatus;
    private List<String> tags;
    private String comment;

    /**
     * 최종 이미지 표시 순서. 배열 순서가 곧 표시 순서이며 서버가 이 위치로 imageOrder(1-base)를 재부여한다.
     * 생략(null) 시 기존 순서 유지 + 신규 files는 뒤에 append.
     * 빈 배열([])은 "순서 지정 없음"으로 처리하며 전체 삭제가 아니다(삭제는 deleteImageIds로만).
     */
    private List<ImageOrderItem> images;
}
