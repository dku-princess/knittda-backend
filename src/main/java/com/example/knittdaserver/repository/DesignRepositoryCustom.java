package com.example.knittdaserver.repository;

import com.example.knittdaserver.dto.DesignDto;

import java.util.List;

public interface DesignRepositoryCustom {
    List<DesignDto> searchByKeyword(List<String> keywords);
}
