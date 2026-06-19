package com.example.knittdaserver.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import com.example.knittdaserver.dto.FeedDto;
import com.example.knittdaserver.dto.ImageDto;
import com.example.knittdaserver.dto.RecordResponse;
import com.example.knittdaserver.dto.FlaskSearchRequest;
import com.example.knittdaserver.dto.FlaskSearchResponse;
import com.example.knittdaserver.dto.SearchResponse;
import com.example.knittdaserver.repository.ImageRepository;
import com.example.knittdaserver.repository.RecordRepository;
import com.example.knittdaserver.entity.Record;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class FeedService {
    private static final Logger log = LoggerFactory.getLogger(FeedService.class);
    private final RecordRepository recordRepository;
    private final ImageRepository imageRepository;
    private final com.example.knittdaserver.service.SearchLogService searchLogService;
    
    @Value("${flask.server.url}")
    private String flaskServerUrl;
    
    // 검색 버전 상수
    public static final String SEARCH_VERSION_V1 = "v1_keyword";
    public static final String SEARCH_VERSION_V5 = "v5_hybrid_5_5";
    public static final String SEARCH_VERSION_V6 = "v6_hybrid_3_7";
    
    private final WebClient webClient = WebClient.builder()
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
        .build();

    /**
     * 전체 Record를 페이징하여 FeedDto로 반환합니다.
     * @param pageable 페이징 정보
     * @return FeedDto의 Page
     */
    @Transactional(readOnly = true)
    public Page<FeedDto> getFeedRecords(org.springframework.data.domain.Pageable pageable) {
        // 1. record 페이지를 DB 페이징으로 조회 (to-one 연관만 fetch, 컬렉션 미포함 → HHH90003004 방지)
        Page<Record> recordPage = recordRepository.findAll(pageable);

        // 2. 페이지 내 record 들의 이미지를 단일 IN 절 쿼리로 일괄 조회 (N+1 방지)
        Map<Long, List<ImageDto>> imagesByRecordId = loadImagesByRecordId(recordPage.getContent());

        // 3. 사전 조회한 이미지를 주입하여 FeedDto 로 변환 (record.getImages() lazy load 미발생)
        return recordPage.map(record -> FeedDto.builder()
            .userName(record.getProject().getUser().getNickname())
            .profileImageUrl(record.getProject().getUser().getProfileImageUrl())
            .projectName(record.getProject().getNickname())
            .projectId(record.getProject().getId())
            .designTitle(record.getProject().getDesign().getTitle())
            .designer(record.getProject().getDesign().getDesigner())
            .record(RecordResponse.from(record, imagesByRecordId.getOrDefault(record.getId(), List.of())))
            .build());
    }

    /**
     * 주어진 record 들의 이미지를 단일 쿼리로 조회하여 recordId 기준 Map 으로 그룹핑합니다.
     * @param records 이미지를 조회할 record 목록
     * @return recordId → ImageDto 리스트 Map
     */
    private Map<Long, List<ImageDto>> loadImagesByRecordId(List<Record> records) {
        if (records.isEmpty()) {
            return Map.of();
        }

        List<Long> recordIds = records.stream()
            .map(Record::getId)
            .collect(Collectors.toList());

        return imageRepository.findByRecordIdIn(recordIds).stream()
            .collect(Collectors.groupingBy(
                image -> image.getRecord().getId(),
                Collectors.mapping(ImageDto::from, Collectors.toList())));
    }

    /**
     * v6: Flask 서버를 통한 Hybrid 검색 (Embedding + ElasticSearch 3:7)
     * searchId와 searchVersion을 포함한 검색 결과를 반환합니다.
     * 실제 사용된 검색 버전을 추적하여 로그에 기록합니다.
     * @param keyword 검색어
     * @param pageable 페이징 정보
     * @param userId 사용자 ID (nullable, JWT에서 추출)
     * @return SearchResponse (searchId, searchVersion, Page 포함)
     */
    public SearchResponse searchFeedRecordsWithLog(String keyword, Pageable pageable, Long userId) {
        // searchId 생성
        String searchId = searchLogService.generateSearchId();
        
        // 검색 수행 및 실제 사용된 버전 추적
        SearchResult result = searchFeedRecordsWithVersionTracking(keyword, pageable);
        
        // 실제 사용된 버전으로 검색 이벤트 로그 저장 (비동기, 실패해도 검색은 진행)
        searchLogService.saveSearchEventLog(searchId, userId, keyword, result.getVersion());
        
        // 검색 결과의 recordId 목록 로깅 (디버깅용)
        List<Long> resultRecordIds = result.getPage().getContent().stream()
            .filter(feed -> feed.getRecord() != null && feed.getRecord().getId() != null)
            .map(feed -> feed.getRecord().getId())
            .collect(Collectors.toList());
        log.info("[FeedService] 검색 결과 recordId 목록 - searchId: {}, recordIds: {}", searchId, resultRecordIds);
        
        // SearchResponse 생성 (실제 사용된 버전 포함)
        return SearchResponse.from(result.getPage(), searchId, result.getVersion());
    }
    
    /**
     * 검색 수행 및 실제 사용된 버전을 추적하는 내부 메서드
     * v6 → v5 → v1 순서로 시도하며, 성공한 버전을 반환합니다.
     * v6와 v5는 동일한 allFeeds 데이터를 공유하여 성능을 최적화합니다.
     */
    private SearchResult searchFeedRecordsWithVersionTracking(String keyword, Pageable pageable) {
        // v6와 v5가 공유할 allFeeds를 먼저 생성 (한 번만 조회 및 변환)
        List<FeedDto> sharedAllFeeds = prepareAllFeeds();
        
        if (sharedAllFeeds.isEmpty()) {
            log.info("[SearchVersion] Record가 없어서 빈 결과 반환");
            return new SearchResult(new PageImpl<>(List.of(), pageable, 0), SEARCH_VERSION_V6);
        }
        
        // v6 시도 (공유된 allFeeds 사용)
        try {
            Page<FeedDto> page = searchFeedRecordsInternal(keyword, pageable, sharedAllFeeds);
            log.info("[SearchVersion] v6 사용됨");
            return new SearchResult(page, SEARCH_VERSION_V6);
        } catch (Exception e) {
            log.warn("[SearchVersion] v6 실패, v5로 fallback 시도 - {}", e.getMessage());
        }
        
        // v5 시도 (공유된 allFeeds 재사용)
        try {
            Page<FeedDto> page = searchFeedRecordsV5Internal(keyword, pageable, sharedAllFeeds);
            log.info("[SearchVersion] v5 사용됨");
            return new SearchResult(page, SEARCH_VERSION_V5);
        } catch (Exception e) {
            log.warn("[SearchVersion] v5 실패, v1로 fallback 시도 - {}", e.getMessage());
        }
        
        // v1 시도 (최후 fallback, 항상 성공)
        log.info("[SearchVersion] v1 사용됨 (최후 fallback)");
        Page<FeedDto> page = searchFeedRecordsV1(keyword, pageable);
        return new SearchResult(page, SEARCH_VERSION_V1);
    }
    
    /**
     * 모든 Record를 조회하여 FeedDto 리스트로 변환합니다.
     * v6와 v5가 공유하는 데이터를 생성합니다.
     */
    private List<FeedDto> prepareAllFeeds() {
        List<Record> allRecords = recordRepository.findAllWithAssociations();
        log.info("[FeedService] 전체 Record 조회 및 변환 완료: {}개", allRecords.size());
        
        if (allRecords.isEmpty()) {
            return List.of();
        }
        
        return allRecords.stream()
            .map(record -> FeedDto.builder()
                .userName(record.getProject().getUser().getNickname())
                .profileImageUrl(record.getProject().getUser().getProfileImageUrl())
                .projectName(record.getProject().getNickname())
                .projectId(record.getProject().getId())
                .designTitle(record.getProject().getDesign().getTitle())
                .designer(record.getProject().getDesign().getDesigner())
                .record(RecordResponse.from(record))
                .build())
            .collect(Collectors.toList());
    }
    
    /**
     * v6 검색 내부 구현 (fallback 없음)
     * @param keyword 검색어
     * @param pageable 페이징 정보
     * @param sharedAllFeeds 공유할 allFeeds (null이면 새로 생성)
     */
    private Page<FeedDto> searchFeedRecordsInternal(String keyword, Pageable pageable, List<FeedDto> sharedAllFeeds) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("v6 requires keyword");
        }

        // 1. allFeeds 준비 (공유 데이터가 없으면 생성)
        List<FeedDto> allFeeds = sharedAllFeeds;
        if (allFeeds == null) {
            allFeeds = prepareAllFeeds();
        }
        
        if (allFeeds.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, 0);
            }
            
        // 2. Flask 서버 요청 데이터 생성
            FlaskSearchRequest request = FlaskSearchRequest.builder()
                .keyword(keyword)
                .feeds(allFeeds)
                .build();
            
        // Flask 서버에 보낸 데이터의 recordId 추출 (디버깅용)
        List<Long> sentRecordIds = allFeeds.stream()
            .filter(feed -> feed.getRecord() != null && feed.getRecord().getId() != null)
            .map(feed -> feed.getRecord().getId())
            .collect(Collectors.toList());
        log.info("[v6 search] Flask 서버로 요청 전송 - 키워드: '{}', 데이터 개수: {}개, recordId 범위: {} ~ {}", 
            keyword, allFeeds.size(), 
            sentRecordIds.isEmpty() ? "없음" : sentRecordIds.stream().min(Long::compare).orElse(null),
            sentRecordIds.isEmpty() ? "없음" : sentRecordIds.stream().max(Long::compare).orElse(null));
            
        // 3. Flask 서버로 요청 전송 (v6 엔드포인트 - Hybrid 3:7)
            FlaskSearchResponse response = webClient.post()
            .uri(flaskServerUrl + "/search/v6")
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                    clientResponse -> {
                    log.error("[v6 search] Flask 서버 오류 응답: {}", clientResponse.statusCode());
                        return Mono.error(new RuntimeException("Flask 서버 오류: " + clientResponse.statusCode()));
                    })
                .bodyToMono(FlaskSearchResponse.class)
                .timeout(java.time.Duration.ofSeconds(30))
                .block();
            
            if (response == null || response.getResults() == null) {
            throw new RuntimeException("Flask 서버 응답이 null이거나 결과가 없음");
            }
            
        log.info("[v6 search] Flask 서버 응답 받음: {}개 결과", response.getResults().size());
            
        // 4. Flask 서버 결과를 Spring에서 페이징 처리
            List<FeedDto> allResults = response.getResults();
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), allResults.size());
            List<FeedDto> pageContent = start > end ? List.of() : allResults.subList(start, end);
            
            return new PageImpl<>(pageContent, pageable, allResults.size());
    }
    
    /**
     * v5 검색 내부 구현 (fallback 없음)
     * @param keyword 검색어
     * @param pageable 페이징 정보
     * @param sharedAllFeeds 공유할 allFeeds (null이면 새로 생성)
     */
    private Page<FeedDto> searchFeedRecordsV5Internal(String keyword, Pageable pageable, List<FeedDto> sharedAllFeeds) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("v5 requires keyword");
        }
        
        // 1. allFeeds 준비 (공유 데이터가 없으면 생성)
        List<FeedDto> allFeeds = sharedAllFeeds;
        if (allFeeds == null) {
            allFeeds = prepareAllFeeds();
        }
        
        if (allFeeds.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, 0);
            }
            
        // 2. Flask 서버 요청 데이터 생성
            FlaskSearchRequest request = FlaskSearchRequest.builder()
                .keyword(keyword)
                .feeds(allFeeds)
                .build();
            
            log.info("[v5 search] Flask 서버로 요청 전송 - 키워드: '{}', 데이터 개수: {}개", keyword, allFeeds.size());
            
        // 3. Flask 서버로 요청 전송 (v5 엔드포인트 - Hybrid 5:5)
            FlaskSearchResponse response = webClient.post()
                .uri(flaskServerUrl + "/search/v5")
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                    clientResponse -> {
                        log.error("[v5 search] Flask 서버 오류 응답: {}", clientResponse.statusCode());
                        return Mono.error(new RuntimeException("Flask 서버 오류: " + clientResponse.statusCode()));
                    })
                .bodyToMono(FlaskSearchResponse.class)
                .timeout(java.time.Duration.ofSeconds(30))
                .block();
            
            if (response == null || response.getResults() == null) {
            throw new RuntimeException("Flask 서버 응답이 null이거나 결과가 없음");
            }
            
            log.info("[v5 search] Flask 서버 응답 받음: {}개 결과", response.getResults().size());
            
        // 4. Flask 서버 결과를 Spring에서 페이징 처리
            List<FeedDto> allResults = response.getResults();
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), allResults.size());
            List<FeedDto> pageContent = start > end ? List.of() : allResults.subList(start, end);
            
            return new PageImpl<>(pageContent, pageable, allResults.size());
    }
    
    /**
     * 검색 결과와 사용된 버전을 함께 보관하는 내부 클래스
     */
    private static class SearchResult {
        private final Page<FeedDto> page;
        private final String version;
        
        SearchResult(Page<FeedDto> page, String version) {
            this.page = page;
            this.version = version;
        }
        
        Page<FeedDto> getPage() {
            return page;
        }
        
        String getVersion() {
            return version;
        }
    }
    
    /**
     * v6: Flask 서버를 통한 Hybrid 검색 (Embedding + ElasticSearch 3:7)
     * 1. 모든 Record 데이터를 Flask 서버로 전송
     * 2. Flask 서버에서 Embedding과 ElasticSearch 하이브리드 검색 수행 (3:7 비율)
     * 3. 결과를 받아서 FeedDto로 변환하여 반환
     * @param keyword 검색어
     * @param pageable 페이징 정보
     * @return FeedDto의 Page (Flask 서버 유사도 순)
     */
    public Page<FeedDto> searchFeedRecords(String keyword, Pageable pageable) {
        log.info("[v6 search] 시작 - keyword: '{}', pageable: {}", keyword, pageable);
        
        if (keyword == null || keyword.isBlank()) {
            log.info("[v6 search] 키워드가 비어있어서 전체 조회로 변경");
            return getFeedRecords(pageable);
        }
        
        try {
            return searchFeedRecordsInternal(keyword, pageable, null);
        } catch (Exception e) {
            log.error("[v6 search] Flask 서버 통신 중 오류 발생", e);
            // 오류 발생 시 v5로 fallback
            log.info("[v6 search] 오류로 인해 v5로 fallback");
            return searchFeedRecordsV5(keyword, pageable);
        }
    }

    /**
     * v5: Flask 서버를 통한 Hybrid 검색 (Embedding + ElasticSearch 5:5)
     * 1. 모든 Record 데이터를 Flask 서버로 전송
     * 2. Flask 서버에서 Embedding과 ElasticSearch 하이브리드 검색 수행 (5:5 비율)
     * 3. 결과를 받아서 FeedDto로 변환하여 반환
     * @param keyword 검색어
     * @param pageable 페이징 정보
     * @return FeedDto의 Page (Flask 서버 유사도 순)
     */
    public Page<FeedDto> searchFeedRecordsV5(String keyword, Pageable pageable) {
        log.info("[v5 search] 시작 - keyword: '{}', pageable: {}", keyword, pageable);
        
        if (keyword == null || keyword.isBlank()) {
            log.info("[v5 search] 키워드가 비어있어서 전체 조회로 변경");
            return getFeedRecords(pageable);
        }
        
        try {
            return searchFeedRecordsV5Internal(keyword, pageable, null);
        } catch (Exception e) {
            log.error("[v5 search] Flask 서버 통신 중 오류 발생", e);
            // 오류 발생 시 v1로 fallback
            log.info("[v5 search] 오류로 인해 v1로 fallback");
            return searchFeedRecordsV1(keyword, pageable);
        }
    }

    /**
     * v1: 키워드 기반 단순 텍스트 검색 (태그, 프로젝트명, 도안명 등)
     * 최후 fallback으로 사용됩니다.
     * @param keyword 검색어 (null 또는 빈 값이면 전체 반환)
     * @param pageable 페이징 정보
     * @return FeedDto의 Page (유사도 점수로 정렬)
     */
    public Page<FeedDto> searchFeedRecordsV1(String keyword, org.springframework.data.domain.Pageable pageable) {
        log.info("[v1 search] 시작 - keyword: '{}', pageable: {}", keyword, pageable);
        
        if (keyword == null || keyword.isBlank()) {
            log.info("[v1 search] 키워드가 비어있어서 전체 조회로 변경");
            return getFeedRecords(pageable);
        }
        
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        // 전체 Record를 페이징 없이 모두 가져온다
            List<Record> allRecords = recordRepository.findAllWithAssociations();
        List<FeedDto> filtered = allRecords.stream()
                .map(record -> FeedDto.builder()
                    .userName(record.getProject().getUser().getNickname())
                    .profileImageUrl(record.getProject().getUser().getProfileImageUrl())
                    .projectName(record.getProject().getNickname())
                    .projectId(record.getProject().getId())
                    .designTitle(record.getProject().getDesign().getTitle())
                    .designer(record.getProject().getDesign().getDesigner())
                    .record(RecordResponse.from(record))
                    .build())
            .filter(feedDto -> {
                boolean matchProjectName = feedDto.getProjectName() != null && feedDto.getProjectName().toLowerCase(Locale.ROOT).contains(lowerKeyword);
                boolean matchDesignTitle = feedDto.getDesignTitle() != null && feedDto.getDesignTitle().toLowerCase(Locale.ROOT).contains(lowerKeyword);
                boolean matchDesigner = feedDto.getDesigner() != null && feedDto.getDesigner().toLowerCase(Locale.ROOT).contains(lowerKeyword);
                boolean matchTags = feedDto.getRecord() != null && feedDto.getRecord().getTags() != null && feedDto.getRecord().getTags().stream().anyMatch(tag -> tag != null && tag.toLowerCase(Locale.ROOT).contains(lowerKeyword));
                boolean matchMemo = feedDto.getRecord() != null && feedDto.getRecord().getComment() != null && feedDto.getRecord().getComment().toLowerCase(Locale.ROOT).contains(lowerKeyword);
                return matchProjectName || matchDesignTitle || matchDesigner || matchTags || matchMemo;
            })
                .collect(Collectors.toList());
            
        // 유사도 점수 기반 정렬 추가
        filtered.sort((a, b) -> Integer.compare(getSimilarityScore(b, lowerKeyword), getSimilarityScore(a, lowerKeyword)));
        
        // 페이징 적용
            int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());
        List<FeedDto> pageContent = start > end ? List.of() : filtered.subList(start, end);
        
        log.info("[v1 search] 완료 - 결과 개수: {}, 총 개수: {}", pageContent.size(), filtered.size());
        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    /**
     * v6: Flask 서버를 통한 Hybrid 검색 (Embedding + ElasticSearch 3:7)
     * 별도 메서드로 제공 (내부적으로 searchFeedRecords와 동일)
     * @param keyword 검색어
     * @param pageable 페이징 정보
     * @return FeedDto의 Page (Flask 서버 유사도 순)
     */
    public Page<FeedDto> searchFeedRecordsV6(String keyword, org.springframework.data.domain.Pageable pageable) {
        return searchFeedRecords(keyword, pageable);
    }

    /**
     * 검색어 포함 횟수 기반 유사도 점수 계산
     * @param feedDto FeedDto
     * @param keyword 검색어 (소문자)
     * @return 유사도 점수
     */
    private int getSimilarityScore(FeedDto feedDto, String keyword) {
        int score = 0;
        if (feedDto.getProjectName() != null) {
            score += countOccurrences(feedDto.getProjectName().toLowerCase(Locale.ROOT), keyword);
        }
        if (feedDto.getDesignTitle() != null) {
            score += countOccurrences(feedDto.getDesignTitle().toLowerCase(Locale.ROOT), keyword);
        }
        if (feedDto.getDesigner() != null) {
            score += countOccurrences(feedDto.getDesigner().toLowerCase(Locale.ROOT), keyword);
        }
        if (feedDto.getRecord() != null && feedDto.getRecord().getTags() != null) {
            for (String tag : feedDto.getRecord().getTags()) {
                if (tag != null) {
                    score += countOccurrences(tag.toLowerCase(Locale.ROOT), keyword);
                }
            }
        }
        if (feedDto.getRecord() != null && feedDto.getRecord().getComment() != null) {
            score += countOccurrences(feedDto.getRecord().getComment().toLowerCase(Locale.ROOT), keyword);
        }
        return score;
    }

    /**
     * 문자열 내 검색어 등장 횟수 세기
     * @param text 검색 대상 텍스트
     * @param keyword 검색어
     * @return 등장 횟수
     */
    private int countOccurrences(String text, String keyword) {
        if (keyword.isEmpty()) return 0;
        int count = 0, idx = 0;
        while ((idx = text.indexOf(keyword, idx)) != -1) {
            count++;
            idx += keyword.length();
        }
        return count;
    }
}
