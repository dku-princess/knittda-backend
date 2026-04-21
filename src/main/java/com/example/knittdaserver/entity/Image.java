package com.example.knittdaserver.entity;

import java.time.LocalDateTime;

import org.aspectj.lang.annotation.Before;
import org.hibernate.annotations.CreationTimestamp;

import com.example.knittdaserver.util.S3DeleteHelper;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Table(name = "image")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Record에 연결 (기록 이미지)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", foreignKey = @ForeignKey(name = "fk_image_record"))
    private Record record;

    @Column(name = "image_url", nullable = false, length = 1024)
    private String imageUrl;

    // 기록 이미지에서만 사용
    @Column(name = "image_order")
    private Integer imageOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime createdAt;

    @PreRemove
    public void preRemove() {
        // 이미지 삭제 전 S3에서 파일 삭제
        if (this.imageUrl != null && !this.imageUrl.isEmpty()) {
            S3DeleteHelper.deleteFile(imageUrl);    
        }
    }
}

