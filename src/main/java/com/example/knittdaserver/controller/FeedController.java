package com.example.knittdaserver.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.knittdaserver.common.response.ApiResponse;
import com.example.knittdaserver.dto.FeedDto;
import com.example.knittdaserver.service.FeedService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

@Tag(name = "Feed", description = "피드 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
public class FeedController {
    
    private final FeedService feedService;

    @Operation(summary = "모든 Record 조회", description = "피드에 표시할 모든 Record를 조회합니다.")
    @GetMapping("/")
    public ResponseEntity<ApiResponse<Page<FeedDto>>> getFeedRecords(
        @PageableDefault(size = 20, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<FeedDto> feeds = feedService.getFeedRecords(pageable);
        return ResponseEntity.ok(ApiResponse.success(feeds));
    }

    @GetMapping("/v1/search")
    public ResponseEntity<ApiResponse<Page<FeedDto>>> searchFeedRecordsV1(
        @RequestParam(value = "keyword", required = false) String keyword,
        @PageableDefault(size = 20, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<FeedDto> feeds = feedService.searchFeedRecordsV1(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(feeds));
    }

    @GetMapping("/v2/search")
    public ResponseEntity<ApiResponse<Page<FeedDto>>> searchFeedRecordsV2(
        @RequestParam(value = "keyword") String keyword,
        @PageableDefault(size = 20, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<FeedDto> feeds = feedService.searchFeedRecordsV2(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(feeds));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<FeedDto>>> searchFeedRecords(
        @RequestParam(value = "keyword") String keyword,
        @PageableDefault(size = 20, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<FeedDto> feeds = feedService.searchFeedRecordsV2(keyword, pageable);
        log.info("keyword: {}, feeds: {}", keyword, feeds.getContent());
        return ResponseEntity.ok(ApiResponse.success(feeds));
    }
}
