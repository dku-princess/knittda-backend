package com.example.knittdaserver.repository;

import com.example.knittdaserver.dto.DesignDto;
import com.example.knittdaserver.entity.Design;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DesignRepository extends JpaRepository<Design, Long>, DesignRepositoryCustom {
}

