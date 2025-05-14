package com.example.knittdaserver.service;

import com.example.knittdaserver.dto.DesignDto;
import com.example.knittdaserver.repository.DesignRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DesignQueryService {

    private final DesignRepository designRepository;
    private final JPAQueryFactory queryFactory;

    public List<DesignDto> search(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return designRepository.searchAll();
        }
        String[] keywords = keyword.trim().split("\\s+");

        return designRepository.searchByKeyword(List.of(keywords));
    }
}
