package com.example.knittdaserver.entity;

import com.example.knittdaserver.dto.UpdateProjectRequest;
import com.fasterxml.jackson.annotation.JsonFormat;
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

    @OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true)
    @JoinColumn(name = "design_id", nullable = false)
    @ToString.Exclude
    private Design design;

    @JoinColumn(name = "thumbnail")
    @OneToOne(fetch = FetchType.LAZY)
    private ThumbnailImage thumbnail;

    @OneToMany(mappedBy = "project", cascade = CascadeType.PERSIST, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    @Builder.Default private List<Record> records = new ArrayList<>(0);

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_project_user"))
    private User user;
    
    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.IN_PROGRESS;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "last_record_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime lastRecordAt;

    @Column(name = "start_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Column(name = "end_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @Column(name = "goal_date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate goalDate;

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

    public void setDesign(Design design) {
        this.design = design;
    }

    public void setLastRecordAt(LocalDateTime lastRecordAt) {
        this.lastRecordAt = lastRecordAt;
    }

    public void setThumbnail(ThumbnailImage thumbnail) {
        this.thumbnail = thumbnail;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }
}
