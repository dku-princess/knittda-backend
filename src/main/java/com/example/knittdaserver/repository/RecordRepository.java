package com.example.knittdaserver.repository;


import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.knittdaserver.entity.Record;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface RecordRepository extends JpaRepository<Record, Long> {

    @Query("SELECT r FROM Record r where r.project.user.id = :userId")
    List<Record> findAllByUserId(@Param("userId") Long userId);

    List<Record> findByProjectId(Long projectId);

    @Query("SELECT r FROM Record r " +
            "WHERE r.project.user.id = :userId " +
            "AND r.createdAt >= :startOfWeek " +
            "AND r.createdAt < :endOfWeek")
    List<Record> findWeeklyRecordsByUserId(
            Long userId,
            LocalDateTime startOfWeek,
            LocalDateTime endOfWeek
    );

    @EntityGraph(attributePaths = {"project", "project.user", "images", "project.design"})
    Page<Record> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"project", "project.user", "project.design"})
//     @EntityGraph(attributePaths = {"project", "project.user", "images", "project.design"})
    @Query("SELECT DISTINCT r FROM Record r")
    List<Record> findAllWithAssociations();
}
