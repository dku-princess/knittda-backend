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

    // mapToPreviews 2단계: 주어진 record 들의 첫 번째(imageOrder=1) 이미지만 조회.
    // record 당 이미지가 5장 이내라 record_id 필터만으로 충분히 가벼움 (윈도우 함수 불필요).
    @Query("SELECT i.record.id AS recordId, i.imageUrl AS imageUrl " +
            "FROM Image i WHERE i.record.id IN :recordIds AND i.imageOrder = 1")
    List<RecordFirstImageProjection> findFirstImageByRecordIds(@Param("recordIds") Collection<Long> recordIds);

    interface RecordFirstImageProjection {
        Long getRecordId();
        String getImageUrl();
    }
}
    