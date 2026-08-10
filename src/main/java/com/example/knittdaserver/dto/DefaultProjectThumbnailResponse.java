package com.example.knittdaserver.dto;

import com.example.knittdaserver.entity.DefaultProjectThumbnail;

import lombok.Builder;
import lombok.Getter;

/**
 * 기본 이미지 선택지 목록 응답.
 * FE 는 id 를 프로젝트 생성/수정 요청의 defaultThumbnailId 로 전달한다.
 */
@Getter
@Builder
public class DefaultProjectThumbnailResponse {

    private Long id;
    private String name;
    private String imageUrl;

    public static DefaultProjectThumbnailResponse from(DefaultProjectThumbnail entity) {
        return DefaultProjectThumbnailResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .imageUrl(entity.getImageUrl())
                .build();
    }
}
