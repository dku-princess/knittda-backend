package com.example.knittdaserver.dto;

public record ProjectArticlePreviewResponse(
        Long projectId,
        String projectName,
        String thumbnailUrl,
        String nickname
) {}
