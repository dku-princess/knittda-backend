package com.example.knittdaserver.repository;


import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.knittdaserver.entity.Record;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface RecordRepository extends JpaRepository<Record, Long> {

    @Query("SELECT r FROM Record r where r.project.user.id = :userId")
    List<Record> findAllByUserId(@Param("userId") Long userId);

    List<Record> findByProjectId(Long projectId);

    List<Record> findByProjectIdOrderByCreatedAtDesc(Long projectId);

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
    @Query("SELECT DISTINCT r FROM Record r")
    List<Record> findAllWithAssociations();

    // N+1 방지: 프로젝트별 record 개수를 한 번의 집계 쿼리로 조회.
    // mapToPreviews 에서 project.getRecords().size() 대신 사용.
    @Query("SELECT r.project.id AS projectId, COUNT(r.id) AS cnt " +
            "FROM Record r WHERE r.project.id IN :projectIds " +
            "GROUP BY r.project.id")
    List<RecordCountProjection> countByProjectIds(@Param("projectIds") Collection<Long> projectIds);

    interface RecordCountProjection {
        Long getProjectId();
        Long getCnt();
    }
}
