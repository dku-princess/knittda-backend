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
    // 데이터 결함으로 한 record 에 imageOrder=1 이 중복될 수 있어 id ASC 로 정렬해
    // (호출부에서 첫 번째로 만난 값을 채택하는 방식으로) 결정적으로 하나만 고르도록 한다.
    @Query("SELECT i.record.id AS recordId, i.imageUrl AS imageUrl " +
            "FROM Image i WHERE i.record.id IN :recordIds AND i.imageOrder = 1 " +
            "ORDER BY i.record.id ASC, i.id ASC")
    List<RecordFirstImageProjection> findFirstImageByRecordIds(@Param("recordIds") Collection<Long> recordIds);

    interface RecordFirstImageProjection {
        Long getRecordId();
        String getImageUrl();
    }
}
    