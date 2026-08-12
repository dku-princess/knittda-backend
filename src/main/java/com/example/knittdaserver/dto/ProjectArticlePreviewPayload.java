package com.example.knittdaserver.dto;

import java.util.List;

/**
 * 아티클 project 미리보기 API 전용 응답 본문.
 * {@code num}이 0이면 FE에서 해당 섹션을 렌더하지 않으면 됩니다.
 */
public record ProjectArticlePreviewPayload(
        int num,
        List<ProjectArticlePreviewResponse> projects
) {}
