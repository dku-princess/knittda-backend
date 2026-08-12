package com.example.knittdaserver.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.*;
import lombok.*;

/**
 * 작품(프로젝트) 대표사진 미지정 시 선택할 수 있는 기본 이미지 마스터 테이블.
 * 운영자가 관리하는 공용 정적 자산이므로 사용자 콘텐츠와 분리하며,
 * 이미지 파일은 S3(cms/default-project-thumbnail/)에 고정 저장된다.
 * 여기 저장된 image_url 은 여러 프로젝트가 공유 참조하므로 절대 S3에서 삭제하지 않는다.
 */
@Entity
@Getter
@Setter
@Table(name = "default_project_thumbnail")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DefaultProjectThumbnail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 뜨개 종류명 (예: 양말, 목도리, 스웨터) */
    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "image_url", nullable = false, length = 1024)
    private String imageUrl;

    /** FE 선택 화면 노출 순서 (오름차순) */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    /** 노출 여부 — 재배포 없이 on/off */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime createdAt;
}
