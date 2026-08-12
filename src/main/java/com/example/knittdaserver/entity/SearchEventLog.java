package com.example.knittdaserver.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_event_log", indexes = {
    @Index(name = "idx_search_id", columnList = "search_id"),
    @Index(name = "idx_keyword", columnList = "keyword"),
    @Index(name = "idx_search_version", columnList = "search_version")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SearchEventLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "search_id", nullable = false, length = 36)
    private String searchId;
    
    @Column(name = "user_id", nullable = true)
    private Long userId;
    
    @Column(name = "keyword", nullable = false, length = 255)
    private String keyword;
    
    @Column(name = "search_version", nullable = false, length = 50)
    private String searchVersion;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

