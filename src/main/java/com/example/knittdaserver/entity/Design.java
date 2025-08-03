package com.example.knittdaserver.entity;


import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.example.knittdaserver.dto.UpdateProjectRequest;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Design {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "design")
    @ToString.Exclude
    private Project project;

    @Size(max=255)
    private String title;

    @Size(max=255)
    private String designer;

    @Lob
    @Column(columnDefinition = "TEXT", name = "needle_info")
    private String needleInfo;

    @Lob
    @Column(columnDefinition = "TEXT", name = "yarn_info")
    private String yarnInfo;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    @Builder.Default private Boolean visible = true; // default value is true

    @CreationTimestamp
    private LocalDateTime createdAt;

    public void updateFromRequest(UpdateProjectRequest request) {
        if (request.getDesignTitle() != null) {
            this.title = request.getDesignTitle();
        }
        if (request.getDesigner() != null) {
            this.designer = request.getDesigner();
        }
        if (request.getYarnInfo() != null) {
            this.yarnInfo = request.getYarnInfo();
        }
        if (request.getNeedleInfo() != null) {
            this.needleInfo = request.getNeedleInfo();
        }
        if (request.getDescription() != null) {
            this.description = request.getDescription();
        }
        if (request.getVisible() != null) {
            this.visible = request.getVisible();
        }
    }

    
}
