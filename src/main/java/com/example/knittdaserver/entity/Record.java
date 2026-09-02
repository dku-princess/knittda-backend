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
// ddl-auto=update 환경(local, staging)에서는 Hibernate가 자동 생성하지만,
// prod(ddl-auto=validate)에서는 자동 생성되지 않으므로 sql/2026-09-02_add_index_record_created_at.sql을
// 배포 전 RDS(knittda_db)에 수동으로 실행해야 한다 (Flyway 미도입, 수동 운영).
@Entity
@Table(name = "record", indexes = {
        @Index(name = "idx_record_created_at", columnList = "created_at")
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

    // LAZY + BatchSize: 필요한 곳에서만 로딩하되, 한 쿼리에서 여러 record의 tags를
    // 개별 N+1 대신 IN절 한 번으로 묶어 가져온다. size는 페이지 상한(size=2000)보다
    // 작지만 record당 태그가 1~4개뿐이라 결과셋 부담 없이 실사용 트래픽을 커버하는 값.
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
