package com.example.knittdaserver.repository;

import com.example.knittdaserver.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByUserId(Long userId);
    List<Project> findByDesignId(Long designId);
    List<Project> findAllByOrderByLastRecordAtDesc();
}
