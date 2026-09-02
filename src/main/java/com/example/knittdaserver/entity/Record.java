package com.example.knittdaserver.entity;

import com.example.knittdaserver.dto.UpdateRecordRequest;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// created_at 인덱스: /feed 기본 정렬(createdAt DESC)이 필터 없이 전체 테이블을 정렬해야 해서 추가.
// project_id+created_at 복합 인덱스: mapToPreviews 에서 프로젝트별 최신 record 를 찾을 때
// (project_id IN (...) 필터 + project_id 별 created_at DESC 정렬)에 사용, filesort 방지.
@Entity
@Table(name = "record", indexes = {
        @Index(name = "idx_record_created_at", columnList = "created_at"),
        @Index(name = "idx_record_project_created_at", columnList = "project_id, created_at DESC")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Record {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_record_project"))
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordStatus recordStatus;

    @ElementCollection(fetch = FetchType.LAZY)
    @BatchSize(size = 100)
    @CollectionTable(name = "record_tags", joinColumns = @JoinColumn(name = "record_id"))
    @Column(name = "tags")
    @Builder.Default private List<String> tags = new ArrayList<>();

    @Column(name = "comment", columnDefinition = "TEXT", nullable = false)
    private String comment;

    @Column(name = "embedding_json", columnDefinition = "TEXT")
    private String embeddingJson;

    @CreationTimestamp
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "record", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("imageOrder ASC")
    private List<Image> images = new ArrayList<>(0);


    @UpdateTimestamp
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    @Column(name = "updated_at", updatable = true)
    private LocalDateTime updatedAt;

    public void updateFromRequest(UpdateRecordRequest request) {


        if (request.getRecordStatus() != null) {
            this.recordStatus = RecordStatus.fromString(request.getRecordStatus());
        }

        if (request.getTags() != null) {
            this.tags = new ArrayList<>(request.getTags());
        }

        if (request.getComment() != null) {
            this.comment = request.getComment();
        }
    }

    public void setEmbeddingJson(String embeddingJson) {
        this.embeddingJson = embeddingJson;
    }
}
