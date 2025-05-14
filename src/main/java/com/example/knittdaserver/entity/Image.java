package com.example.knittdaserver.entity;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id")
    private Record record;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "image_url", nullable = false, length = 1024)
    private String imageUrl;

    @Column(name = "image_order", nullable = true)
    private Long imageOrder = 1L;

    public void setProject(Project project) {
        this.project = project;
        this.record = null;
    }

    public void setRecord(Record record) {
        this.record = record;
        this.project = null;
    }
    public boolean isProjectImage() {
        return project != null && record == null;
    }

    public boolean isRecordImage() {
        return record != null && project != null;
    }

}
