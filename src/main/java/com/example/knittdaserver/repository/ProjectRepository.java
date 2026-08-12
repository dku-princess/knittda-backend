package com.example.knittdaserver.repository;

import com.example.knittdaserver.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByUserId(Long userId);
    List<Project> findByDesignId(Long designId);
    List<Project> findAllByOrderByLastRecordAtDesc();
    // N+1 방지: user, thumbnail 을 메인 쿼리에서 LEFT JOIN 으로 함께 fetch.
    // (records 컬렉션은 페이지네이션과 같이 fetch 하면 메모리 페이징(HHH000104) 위험이 있어 제외)
    @EntityGraph(attributePaths = {"user", "thumbnail"})
    Page<Project> findPageByOrderByLastRecordAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "thumbnail"})
    List<Project> findAllByIdIn(Collection<Long> ids);
}
