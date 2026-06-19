package com.example.knittdaserver.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectPreviewResponse {
    private Long projectId;
    private String userName;
    private String projectName;
    private int recordNum;
    private LocalDateTime lastRecordAt;

    // DEPRECATED: 2026-06-19. 클라이언트는 recentImageUrl(단일) 사용으로 마이그레이션.
    // 구버전 클라이언트는 List[0] 으로 접근하므로 항상 0~1개 원소를 담아 호환 유지.
    // 제거 검토: 구버전 활성 사용자 < 1% 또는 2026-12-19.
    private List<String> recentImageUrls;

    // 단일 대표 이미지 URL. recentImageUrls 와 동일한 값이 들어감.
    private String recentImageUrl;
}
