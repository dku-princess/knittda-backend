package com.example.knittdaserver.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import com.example.knittdaserver.common.response.ApiResponse;
import com.example.knittdaserver.common.response.ApiResponseCode;
import com.example.knittdaserver.common.response.CustomException;
import com.example.knittdaserver.dto.FeedDto;
import com.example.knittdaserver.dto.SearchResponse;
import com.example.knittdaserver.dto.SearchClickLogRequest;
import com.example.knittdaserver.service.FeedService;
import com.example.knittdaserver.service.SearchLogService;
import com.example.knittdaserver.service.AuthService;

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
    private final SearchLogService searchLogService;
    private final AuthService authService;

    @Operation(summary = "모든 Record 조회", description = "피드에 표시할 모든 Record를 조회합니다.")
    @GetMapping("/")
    public ResponseEntity<ApiResponse<Page<FeedDto>>> getFeedRecords(
        @PageableDefault(size = 20, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<FeedDto> feeds = feedService.getFeedRecords(pageable);
        return ResponseEntity.ok(ApiResponse.success(feeds));
    }

    @Operation(summary = "검색", description = "키워드로 Record를 검색합니다. searchId와 searchVersion이 응답에 포함됩니다.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<SearchResponse>> searchFeedRecords(
        @RequestParam(value = "keyword") String keyword,
        @RequestHeader(value = "Authorization", required = false) String token,
        @PageableDefault(size = 20, page = 0, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        // JWT에서 userId 추출 (optional)
        Long userId = null;
        if (token != null && !token.isBlank()) {
            try {
                userId = authService.getUserFromJwt(token).getId();
            } catch (Exception e) {
                log.debug("JWT 인증 실패 (무시하고 계속 진행): {}", e.getMessage());
            }
        }
        
        SearchResponse response = feedService.searchFeedRecordsWithLog(keyword, pageable, userId);
        
        log.info("검색 완료 - searchId: {}, keyword: '{}', 결과 개수: {}, 총 개수: {}, 페이지: {}/{}", 
            response.getSearchId(), keyword, response.getContent().size(), response.getTotalElements(), 
            response.getNumber() + 1, response.getTotalPages());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @Operation(summary = "검색 결과 클릭 로그 수집", description = "검색 결과에서 Record를 클릭했을 때 로그를 저장합니다.")
    @PostMapping("/search/log/click")
    public ResponseEntity<ApiResponse<Void>> saveSearchClickLog(
        @Valid @RequestBody SearchClickLogRequest request,
        @RequestHeader(value = "Authorization", required = false) String token) {
        
        // searchId 존재 여부 확인
        if (!searchLogService.existsSearchId(request.getSearchId())) {
            throw new CustomException(ApiResponseCode.INVALID_INPUT);
        }
        
        // JWT에서 userId 추출 (optional)
        Long userId = null;
        if (token != null && !token.isBlank()) {
            try {
                userId = authService.getUserFromJwt(token).getId();
            } catch (Exception e) {
                log.debug("JWT 인증 실패 (무시하고 계속 진행): {}", e.getMessage());
            }
        }
        
        // searchVersion 조회
        String searchVersion = searchLogService.getSearchVersionBySearchId(request.getSearchId());
        if (searchVersion == null) {
            log.warn("searchId에 해당하는 searchVersion을 찾을 수 없음 - searchId: {}", request.getSearchId());
            searchVersion = "unknown";
        }
        
        // 클릭 로그 저장 (비동기, 실패해도 예외 전파 안 함)
        searchLogService.saveSearchClickLog(request, userId, searchVersion);
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
