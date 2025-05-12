package com.example.knittdaserver.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import com.example.knittdaserver.entity.Record;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecordRepository extends JpaRepository<Record, Long> {

    @Query("SELECT r FROM Record r where r.project.user.id = :userId")
    List<Record> findAllByUserId(@Param("userId") Long userId);

    List<Record> findByProjectId(Long projectId);
}
