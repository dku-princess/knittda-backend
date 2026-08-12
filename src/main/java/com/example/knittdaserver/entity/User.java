package com.example.knittdaserver.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="kakao_id", unique=true, nullable = true)
    private Long kakaoId;

    @Column(name="apple_id", unique=true, nullable = true)
    private String appleId;

    @Column(nullable = true)
    private String email;

    @Column(name = "name", nullable = true)
    private String name;

    @Column(nullable = true)
    private String nickname;

    @Column(name="profile_image_url", nullable = true)
    private String profileImageUrl;

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    @CreationTimestamp
    @Column(name="created_at", nullable = true, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @Builder.Default private List<Project> projects = new ArrayList<>(0);
}
