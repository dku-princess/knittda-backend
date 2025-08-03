package com.example.knittdaserver.repository;

import com.example.knittdaserver.entity.Design;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DesignRepository extends JpaRepository<Design, Long>, DesignRepositoryCustom {
}

