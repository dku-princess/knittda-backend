package com.example.knittdaserver.repository;

import com.example.knittdaserver.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Long> {
}
