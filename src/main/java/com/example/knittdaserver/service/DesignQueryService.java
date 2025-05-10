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

    public List<DesignDto> searchV1(String title, String designer) {
        if (StringUtils.hasText(title) && StringUtils.hasText(designer)) {
            return designRepository.searchByTitleAndDesigner(title, designer);
        }
        if (StringUtils.hasText(title)) {
            return designRepository.searchByTitle(title);
        }
        if (StringUtils.hasText(designer)) {
            return designRepository.searchByDesigner(designer);
        }

        return designRepository.searchAll();
    }

    public List<DesignDto> searchV2(String keyword) {
        String[] keywords = keyword.trim().split("\\s+");
        log.info("Searching for " + Arrays.toString(keywords) + " designs" );
        if (keywords.length > 2) {
            throw new IllegalArgumentException("최대 2개의 키워드만 입력 가능합니다.");
        }
        if (keywords.length == 1) {
            return designRepository.searchSingleKeyword(keywords[0]);
        }
        if (keywords.length == 2) {
            return designRepository.searchTwoKeywords(keywords[0], keywords[1]);
        }
        return designRepository.searchAll();
    }

    public List<DesignDto> search(String keyword) {
        String[] keywords = keyword.trim().split("\\s+");
        if (keywords.length == 0) {
            return designRepository.searchAll();
        }
        return designRepository.searchByKeyword(List.of(keywords));
    }
}
