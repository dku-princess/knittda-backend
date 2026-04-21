package com.example.knittdaserver.repository;

import com.example.knittdaserver.entity.SearchEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SearchEventLogRepository extends JpaRepository<SearchEventLog, Long> {
    boolean existsBySearchId(String searchId);
    Optional<SearchEventLog> findBySearchId(String searchId);
}

