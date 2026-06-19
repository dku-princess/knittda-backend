package com.example.knittdaserver.repository;

import com.example.knittdaserver.entity.Image;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByRecordProjectId(Long projectId);
    List<Image> findByRecordProjectIdOrderByCreatedAtDesc(Long projectId);
    List<Image> findTop1ByRecordIdInOrderByCreatedAtDesc(List<Long> recordIds);

    // N+1 방지: 여러 record 의 이미지를 단일 IN 절 쿼리로 일괄 조회.
    // 피드 페이징 시 record 별 images lazy load(페이지당 N회)를 1회로 줄인다.
    @Query("SELECT i FROM Image i WHERE i.record.id IN :recordIds ORDER BY i.id ASC")
    List<Image> findByRecordIdIn(@Param("recordIds") Collection<Long> recordIds);

    // N+1 방지: 여러 프로젝트의 "최신 record 의 첫 이미지" 를 단일 native query 로 조회.
    // 윈도우 함수(ROW_NUMBER) 로 project_id 별 가장 최근 image 1건만 추림. MySQL 8+ 필요.
    @Query(value =
            "SELECT t.project_id AS projectId, t.image_url AS imageUrl FROM (" +
            "  SELECT r.project_id, i.image_url, " +
            "         ROW_NUMBER() OVER (PARTITION BY r.project_id ORDER BY i.created_at DESC) AS rn " +
            "  FROM image i " +
            "  JOIN record r ON i.record_id = r.id " +
            "  WHERE r.project_id IN (:projectIds)" +
            ") t WHERE t.rn = 1",
            nativeQuery = true)
    List<ProjectLatestImageProjection> findLatestImagePerProject(@Param("projectIds") Collection<Long> projectIds);

    interface ProjectLatestImageProjection {
        Long getProjectId();
        String getImageUrl();
    }
}
    