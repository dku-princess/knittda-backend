package com.example.knittdaserver.repository;

import com.example.knittdaserver.entity.ThumbnailImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ThumbnailImageRepository extends JpaRepository<ThumbnailImage, Long> {
} 