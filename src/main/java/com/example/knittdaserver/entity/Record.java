package com.example.knittdaserver.entity;

import com.example.knittdaserver.dto.UpdateRecordRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
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
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordStatus recordStatus;

    @ElementCollection
    private List<String> tags;

    @Column(name = "comment", columnDefinition = "TEXT", nullable = false)
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @OneToMany(mappedBy = "record", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images = new ArrayList<>();
    public void updateFromRequest(UpdateRecordRequest request) {
        if (request.getProject() != null) {
            this.project = request.getProject();
        }

        if (request.getRecordStatus() != null) {
            this.recordStatus = request.getRecordStatus();
        }

        if (request.getTags() != null) {
            this.tags = request.getTags();
        }

        if (request.getComment() != null) {
            this.comment = request.getComment();
        }
    }
}
