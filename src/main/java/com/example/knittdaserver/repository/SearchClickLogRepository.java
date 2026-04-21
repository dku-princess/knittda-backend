package com.example.knittdaserver.repository;

import com.example.knittdaserver.entity.SearchClickLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchClickLogRepository extends JpaRepository<SearchClickLog, Long> {
}

