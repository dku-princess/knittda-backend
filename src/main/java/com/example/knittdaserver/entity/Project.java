package com.example.knittdaserver.entity;

import com.example.knittdaserver.dto.UpdateProjectRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_id", nullable = false)
    private Design design;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.IN_PROGRESS;

    @Column(name = "custom_yarn_info", columnDefinition = "TEXT")
    private String customYarnInfo;

    @Column(name = "custom_needle_info", columnDefinition = "TEXT")
    private String customNeedleInfo;

    @Column(name = "last_record_at")
    private LocalDateTime lastRecordAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "goal_date")
    private LocalDate goalDate;

    @Builder.Default
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Record> records = new ArrayList<>();

    @OneToOne(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private Image image;
    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }

    public void updateFromRequest(UpdateProjectRequest request) {
        if (request.getNickname() != null) {
            this.nickname = request.getNickname();
        }

        if (request.getStatus() != null) {
            this.status = request.getStatus();
        }

        if (request.getCustomYarnInfo() != null) {
            this.customYarnInfo = request.getCustomYarnInfo();
        }

        if (request.getCustomNeedleInfo() != null) {
            this.customNeedleInfo = request.getCustomNeedleInfo();
        }

        if (request.getLastRecordAt() != null) {
            this.lastRecordAt = request.getLastRecordAt();
        }

        if (request.getStartDate() != null) {
            this.startDate = request.getStartDate();
        }

        if (request.getEndDate() != null) {
            this.endDate = request.getEndDate();
        }

        if (request.getGoalDate() != null) {
            this.goalDate = request.getGoalDate();
        }
    }

    public void setImage(Image image) {
        this.image = image;
        if(image != null) {
            image.setProject(this);
        }
    }

    public void setDesign(Design design) {
        this.design = design;
    }
}
