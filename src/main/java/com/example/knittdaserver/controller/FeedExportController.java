package com.example.knittdaserver.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.knittdaserver.service.FeedService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import com.example.knittdaserver.dto.FeedDto;

@RestController
@RequiredArgsConstructor
public class FeedExportController {

    private final FeedService feedService;
    private final ObjectMapper objectMapper;

    @GetMapping("/export/feeds")
    public ResponseEntity<String> exportFeedsAsJson(
        @PageableDefault(size = 1000) org.springframework.data.domain.Pageable pageable) throws JsonProcessingException {

        Page<FeedDto> feedPage = feedService.getFeedRecords(pageable); // 기존 메서드 그대로 사용
        List<FeedDto> feedList = feedPage.getContent();

        String json = objectMapper
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(feedList);

        return ResponseEntity.ok(json);
    }
}
