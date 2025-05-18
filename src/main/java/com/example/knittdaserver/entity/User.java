package com.example.knittdaserver.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@ToString
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="kakao_id", unique=true, nullable = false)
    private Long kakaoId;

    @Column(nullable = true)
    private String email;

    @Column(nullable = true)
    private String nickname;

    @Column(name="profile_image_url", nullable = true)
    private String profileImageUrl;

    @CreationTimestamp
    @Column(name="created_at", nullable = true, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
