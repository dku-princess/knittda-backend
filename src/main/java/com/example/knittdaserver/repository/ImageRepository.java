package com.example.knittdaserver.repository;

import com.example.knittdaserver.entity.Image;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByRecordProjectId(Long projectId);
    List<Image> findByRecordProjectIdOrderByCreatedAtDesc(Long projectId);
    List<Image> findTop3ByRecordIdInOrderByCreatedAtDesc(List<Long> recordIds);
}
    