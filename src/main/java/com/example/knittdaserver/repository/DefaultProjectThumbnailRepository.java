package com.example.knittdaserver.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.knittdaserver.entity.DefaultProjectThumbnail;

@Repository
public interface DefaultProjectThumbnailRepository extends JpaRepository<DefaultProjectThumbnail, Long> {

    /** FE 선택 화면용 — 활성화된 기본 이미지를 노출 순서대로 조회 */
    List<DefaultProjectThumbnail> findByActiveTrueOrderBySortOrderAsc();

    /** 사용자가 선택한 기본 이미지 조회 (비활성 이미지는 선택 불가) */
    Optional<DefaultProjectThumbnail> findByIdAndActiveTrue(Long id);
}
