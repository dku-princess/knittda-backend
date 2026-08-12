package com.example.knittdaserver.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.example.knittdaserver.util.S3DeleteHelper;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Table(name = "thumbnail_image")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ThumbnailImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "thumbnail")
    private Project project;

    @Column(name = "image_url", nullable = false, length = 1024)
    private String imageUrl;

    /**
     * 기본 이미지(공용 자산) 참조 여부.
     * true 인 경우 imageUrl 은 여러 프로젝트가 공유하는 기본 이미지이므로 S3에서 삭제하면 안 된다.
     */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime createdAt;

    @PreRemove
    public void preRemove() {
        // 기본 이미지는 공용 자산이므로 S3에서 삭제하지 않는다.
        if (this.isDefault) {
            return;
        }
        // 이미지 삭제 전 S3에서 파일 삭제
        if (this.imageUrl != null && !this.imageUrl.isEmpty()) {
            S3DeleteHelper.deleteFile(imageUrl);
        }
    }
} 