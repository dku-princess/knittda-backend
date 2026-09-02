package com.example.knittdaserver.repository;


import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.knittdaserver.entity.Image;
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

    // images는 컬렉션(1:N)이라 fetch join하면 Hibernate가 SQL LIMIT/OFFSET을 적용하지 못하고
    // 전체 로우를 읽어 메모리에서 페이징한다. 페이징 대상 쿼리에서는 컬렉션을 절대 fetch join하지 않는다.
    @EntityGraph(attributePaths = {"project", "project.user", "project.design"})
    Page<Record> findAll(Pageable pageable);

    // findAll(pageable)에서 뺀 images를 record id 목록으로 한 번에 배치 조회.
    @Query("SELECT i FROM Image i WHERE i.record.id IN :recordIds ORDER BY i.record.id ASC, i.imageOrder ASC")
    List<Image> findByRecordIdIn(@Param("recordIds") Collection<Long> recordIds);

    @EntityGraph(attributePaths = {"project", "project.user", "project.design"})
    @Query("SELECT DISTINCT r FROM Record r")
    List<Record> findAllWithAssociations();

    // mapToPreviews 1단계: 여러 프로젝트의 "최신 record" id 를 단일 윈도우 함수 쿼리로 조회.
    // record 레벨에서만 스캔하므로(image 까지 조인하지 않음) 스캔량이 훨씬 작음.
    // idx_record_project_created_at(project_id, created_at DESC) 인덱스로 filesort 방지. MySQL 8+ 필요.
    @Query(value =
            "SELECT t.project_id AS projectId, t.record_id AS recordId FROM (" +
            "  SELECT r.project_id, r.id AS record_id, " +
            "         ROW_NUMBER() OVER (PARTITION BY r.project_id ORDER BY r.created_at DESC) AS rn " +
            "  FROM record r " +
            "  WHERE r.project_id IN (:projectIds)" +
            ") t WHERE t.rn = 1",
            nativeQuery = true)
    List<ProjectLatestRecordProjection> findLatestRecordIdPerProject(@Param("projectIds") Collection<Long> projectIds);

    interface ProjectLatestRecordProjection {
        Long getProjectId();
        Long getRecordId();
    }
}
