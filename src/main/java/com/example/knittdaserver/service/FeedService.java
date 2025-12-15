package com.example.knittdaserver.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import com.example.knittdaserver.dto.FeedDto;
import com.example.knittdaserver.dto.RecordResponse;
import com.example.knittdaserver.dto.FlaskSearchRequest;
import com.example.knittdaserver.dto.FlaskSearchResponse;
import com.example.knittdaserver.repository.RecordRepository;
import com.example.knittdaserver.entity.Record;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.embedding.EmbeddingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;


@Service
@RequiredArgsConstructor
public class FeedService {
    private static final Logger log = LoggerFactory.getLogger(FeedService.class);
    private final RecordRepository recordRepository;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;
    
    @Value("${flask.server.url}")
    private String flaskServerUrl;
    
    private final WebClient webClient = WebClient.builder()
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
        .build();

    /**
     * 전체 Record를 페이징하여 FeedDto로 반환합니다.
     * @param pageable 페이징 정보
     * @return FeedDto의 Page
     */
    public Page<FeedDto> getFeedRecords(org.springframework.data.domain.Pageable pageable) {
        return recordRepository.findAll(pageable)
            .map(record -> FeedDto.builder()
                .userName(record.getProject().getUser().getNickname())
                .profileImageUrl(record.getProject().getUser().getProfileImageUrl())
                .projectName(record.getProject().getNickname())
                .projectId(record.getProject().getId())
                .designTitle(record.getProject().getDesign().getTitle())
                .designer(record.getProject().getDesign().getDesigner())
                .record(RecordResponse.from(record))
                .build());
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
            // 1. 모든 Record 조회
            List<Record> allRecords = recordRepository.findAllWithAssociations();
            log.info("[v6 search] 전체 Record 조회 완료: {}개", allRecords.size());
            
            if (allRecords.isEmpty()) {
                log.info("[v6 search] Record가 없어서 빈 결과 반환");
                return new PageImpl<>(List.of(), pageable, 0);
            }
            
            // 2. 모든 Record를 FeedDto로 변환
            List<FeedDto> allFeeds = allRecords.stream()
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
            
        
            // 3. Flask 서버 요청 데이터 생성
            FlaskSearchRequest request = FlaskSearchRequest.builder()
                .keyword(keyword)
                .feeds(allFeeds)
                .build();
            
            log.info("[v6 search] Flask 서버로 요청 전송 - 키워드: '{}', 데이터 개수: {}개", keyword, allFeeds.size());
            
            // 4. Flask 서버로 요청 전송 (v6 엔드포인트 - Hybrid 3:7)
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
                log.warn("[v6 search] Flask 서버 응답이 null이거나 결과가 없음");
                return new PageImpl<>(List.of(), pageable, 0);
            }
            
            log.info("[v6 search] Flask 서버 응답 받음: {}개 결과", response.getResults().size());
            
            // 5. Flask 서버 결과를 Spring에서 페이징 처리
            List<FeedDto> allResults = response.getResults();
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), allResults.size());
            List<FeedDto> pageContent = start > end ? List.of() : allResults.subList(start, end);
            
            return new PageImpl<>(pageContent, pageable, allResults.size());
            
        } catch (Exception e) {
            log.error("[v6 search] Flask 서버 통신 중 오류 발생", e);
            // 오류 발생 시 전체 조회로 fallback
            log.info("[v6 search] 오류로 인해 전체 조회로 fallback");
            return searchFeedRecordsV2(keyword, pageable);
        }
    }

    /**
     * v1: 키워드 기반 단순 텍스트 검색 (태그, 프로젝트명, 도안명 등)
     * @param keyword 검색어 (null 또는 빈 값이면 전체 반환)
     * @param pageable 페이징 정보
     * @return FeedDto의 Page (유사도 점수로 정렬)
     */
    public Page<FeedDto> searchFeedRecordsV1(String keyword, org.springframework.data.domain.Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return getFeedRecords(pageable);
        }
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        // 전체 Record를 페이징 없이 모두 가져온다 (간단 구현)
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
        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    /**
     * v2: OpenAI 임베딩 기반 유사도 검색
     * 1 검색어 임베딩 생성
     * 2 모든 Record의 임베딩과 코사인 유사도 계산
     *3. 유사도 순 정렬 후 페이징 반환
     * @param keyword 검색어 (필수)
     * @param pageable 페이징 정보
     * @return FeedDto의 Page (임베딩 유사도 순)
     */
    public Page<FeedDto> searchFeedRecordsV2(String keyword, Pageable pageable) {
        log.info("[v2 search] 시작 - keyword: '{}', pageable: {}", keyword, pageable);

        if (keyword == null || keyword.isBlank()) {
            log.info("v2 search] 키워드가 비어있어서 전체 조회로 변경");
            return getFeedRecords(pageable);
        }

        float[] queryEmbedding = getEmbeddingFromOpenAI(keyword);

        log.info("[v2 search]2계: 전체 Record 조회 시작");
        List<Record> allRecords = recordRepository.findAllWithAssociations();

        if (allRecords.isEmpty()) {
            log.info("v2 search] Record가 없어서 빈 결과 반환");
            return new PageImpl<>(List.of(), pageable, 0);
        }

        log.info("[v2 search]3계: 임베딩 파싱 및 유사도 계산 시작");
        List<ScoredFeed> scored = allRecords.stream()
            .map(record -> {
                
                float[] embedding = parseEmbedding(record.getEmbeddingJson());
                if (embedding == null) {
                    log.warn("[v2 search] Record ID {}의 임베딩 파싱 실패", record.getId());
                    return new ScoredFeed(record, -1.0);
                }
                double score = cosineSimilarity(queryEmbedding, embedding);
                log.debug("[v2 search] Record ID {} 유사도 점수:[object Object]", record.getId(), score);
                return new ScoredFeed(record, score);
            })
            .filter(sf -> sf.score >= 0)
            .sorted((a, b) -> Double.compare(b.score, a.score))
            .collect(Collectors.toList());

        // 상위 5개 결과 로그 출력
        List<String> top5 = scored.stream()
            .limit(5)
            .map(sf -> String.format("%s(%.4f)",
                sf.record.getProject().getNickname(), sf.score))
            .collect(Collectors.toList());
        log.info("[v2 search] 상위 5개 결과: {}", top5);



        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), scored.size());
        List<FeedDto> result = (start > end) ? List.of() : scored.subList(start, end).stream()
            .map(sf -> toFeedDto(sf.record, sf.score))
            .collect(Collectors.toList());


        return new PageImpl<>(result, pageable, scored.size());
    }


    /**
     * v3: Flask 서버를 통한 검색
     * 1. 모든 Record 데이터를 Flask 서버로 전송
     * 2. Flask 서버에서 유사도 검색 수행
     * 3. 결과를 받아서 FeedDto로 변환하여 반환
     * @param keyword 검색어
     * @param pageable 페이징 정보
     * @return FeedDto의 Page (Flask 서버 유사도 순)
     */
    public Page<FeedDto> searchFeedRecordsV3(String keyword, org.springframework.data.domain.Pageable pageable) {
        log.info("[v3 search] 시작 - keyword: '{}', pageable: {}", keyword, pageable);
        
        if (keyword == null || keyword.isBlank()) {
            log.info("[v3 search] 키워드가 비어있어서 전체 조회로 변경");
            return getFeedRecords(pageable);
        }
        
        try {
            // 1. 모든 Record 조회
            List<Record> allRecords = recordRepository.findAllWithAssociations();
            log.info("[v3 search] 전체 Record 조회 완료: {}개", allRecords.size());
            
            if (allRecords.isEmpty()) {
                log.info("[v3 search] Record가 없어서 빈 결과 반환");
                return new PageImpl<>(List.of(), pageable, 0);
            }
            
            // 2. 모든 Record를 FeedDto로 변환
            List<FeedDto> allFeeds = allRecords.stream()
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
            
        
            // 3. Flask 서버 요청 데이터 생성
            FlaskSearchRequest request = FlaskSearchRequest.builder()
                .keyword(keyword)
                .feeds(allFeeds)
                .build();
            
            log.info("[v3 search] Flask 서버로 요청 전송 - 키워드: '{}', 데이터 개수: {}개", keyword, allFeeds.size());
            
            // 4. Flask 서버로 요청 전송
            FlaskSearchResponse response = webClient.post()
                .uri(flaskServerUrl + "/search/v3")
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                    clientResponse -> {
                        log.error("[v3 search] Flask 서버 오류 응답: {}", clientResponse.statusCode());
                        return Mono.error(new RuntimeException("Flask 서버 오류: " + clientResponse.statusCode()));
                    })
                .bodyToMono(FlaskSearchResponse.class)
                .timeout(java.time.Duration.ofSeconds(30))
                .block();
            
            if (response == null || response.getResults() == null) {
                log.warn("[v3 search] Flask 서버 응답이 null이거나 결과가 없음");
                return new PageImpl<>(List.of(), pageable, 0);
            }
            
            log.info("[v3 search] Flask 서버 응답 받음: {}개 결과", response.getResults().size());
            
            // 5. Flask 서버 결과를 Spring에서 페이징 처리
            List<FeedDto> allResults = response.getResults();
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), allResults.size());
            List<FeedDto> pageContent = start > end ? List.of() : allResults.subList(start, end);
            
            return new PageImpl<>(pageContent, pageable, allResults.size());
            
        } catch (Exception e) {
            log.error("[v3 search] Flask 서버 통신 중 오류 발생", e);
            // 오류 발생 시 전체 조회로 fallback
            log.info("[v3 search] 오류로 인해 전체 조회로 fallback");
            return searchFeedRecordsV2(keyword, pageable);
        }
    }
    
    public Page<FeedDto> searchFeedRecordsV4(String keyword, Pageable pageable) {
        log.info("[v4 search] 시작 - keyword: '{}', pageable: {}", keyword, pageable);
        
        if (keyword == null || keyword.isBlank()) {
            log.info("[v4 search] 키워드가 비어있어서 전체 조회로 변경");
            return getFeedRecords(pageable);
        }
        
        try {
            // 1. 모든 Record 조회
            List<Record> allRecords = recordRepository.findAllWithAssociations();
            log.info("[v4 search] 전체 Record 조회 완료: {}개", allRecords.size());
            
            if (allRecords.isEmpty()) {
                log.info("[v4 search] Record가 없어서 빈 결과 반환");
                return new PageImpl<>(List.of(), pageable, 0);
            }
            
            // 2. 모든 Record를 FeedDto로 변환
            List<FeedDto> allFeeds = allRecords.stream()
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
            
            // 3. Flask 서버 요청 데이터 생성
            FlaskSearchRequest request = FlaskSearchRequest.builder()
                .keyword(keyword)
                .feeds(allFeeds)
                .build();
            
            log.info("[v4 search] Flask 서버로 요청 전송 - 키워드: '{}', 데이터 개수: {}개", keyword, allFeeds.size());
            
            // 4. Flask 서버로 요청 전송 (v4 엔드포인트)
            FlaskSearchResponse response = webClient.post()
                .uri(flaskServerUrl + "/search/v4")
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                    clientResponse -> {
                        log.error("[v4 search] Flask 서버 오류 응답: {}", clientResponse.statusCode());
                        return Mono.error(new RuntimeException("Flask 서버 오류: " + clientResponse.statusCode()));
                    })
                .bodyToMono(FlaskSearchResponse.class)
                .timeout(java.time.Duration.ofSeconds(30))
                .block();
            
            if (response == null || response.getResults() == null) {
                log.warn("[v4 search] Flask 서버 응답이 null이거나 결과가 없음");
                return new PageImpl<>(List.of(), pageable, 0);
            }
            
            log.info("[v4 search] Flask 서버 응답 받음: {}개 결과", response.getResults().size());
            
            // 5. Flask 서버 결과를 Spring에서 페이징 처리
            List<FeedDto> allResults = response.getResults();
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), allResults.size());
            List<FeedDto> pageContent = start > end ? List.of() : allResults.subList(start, end);
            
            return new PageImpl<>(pageContent, pageable, allResults.size());
            
        } catch (Exception e) {
            log.error("[v4 search] Flask 서버 통신 중 오류 발생", e);
            // 오류 발생 시 전체 조회로 fallback
            log.info("[v4 search] 오류로 인해 전체 조회로 fallback");
            return searchFeedRecordsV2(keyword, pageable);
        }
    }

    public Page<FeedDto> searchFeedRecordsV5(String keyword, Pageable pageable) {
        log.info("[v5 search] 시작 - keyword: '{}', pageable: {}", keyword, pageable);
        
        if (keyword == null || keyword.isBlank()) {
            log.info("[v5 search] 키워드가 비어있어서 전체 조회로 변경");
            return getFeedRecords(pageable);
        }
        
        try {
            // 1. 모든 Record 조회
            List<Record> allRecords = recordRepository.findAllWithAssociations();
            log.info("[v5 search] 전체 Record 조회 완료: {}개", allRecords.size());
            
            if (allRecords.isEmpty()) {
                log.info("[v5 search] Record가 없어서 빈 결과 반환");
                return new PageImpl<>(List.of(), pageable, 0);
            }
            
            // 2. 모든 Record를 FeedDto로 변환
            List<FeedDto> allFeeds = allRecords.stream()
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
            
            // 3. Flask 서버 요청 데이터 생성
            FlaskSearchRequest request = FlaskSearchRequest.builder()
                .keyword(keyword)
                .feeds(allFeeds)
                .build();
            
            log.info("[v5 search] Flask 서버로 요청 전송 - 키워드: '{}', 데이터 개수: {}개", keyword, allFeeds.size());
            
            // 4. Flask 서버로 요청 전송 (v5 엔드포인트 - Hybrid 5:5)
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
                log.warn("[v5 search] Flask 서버 응답이 null이거나 결과가 없음");
                return new PageImpl<>(List.of(), pageable, 0);
            }
            
            log.info("[v5 search] Flask 서버 응답 받음: {}개 결과", response.getResults().size());
            
            // 5. Flask 서버 결과를 Spring에서 페이징 처리
            List<FeedDto> allResults = response.getResults();
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), allResults.size());
            List<FeedDto> pageContent = start > end ? List.of() : allResults.subList(start, end);
            
            return new PageImpl<>(pageContent, pageable, allResults.size());
            
        } catch (Exception e) {
            log.error("[v5 search] Flask 서버 통신 중 오류 발생", e);
            // 오류 발생 시 전체 조회로 fallback
            log.info("[v5 search] 오류로 인해 전체 조회로 fallback");
            return searchFeedRecordsV2(keyword, pageable);
        }
    }

    public Page<FeedDto> searchFeedRecordsV6(String keyword, org.springframework.data.domain.Pageable pageable) {
        log.info("[v6 search] 시작 - keyword: '{}', pageable: {}", keyword, pageable);
        
        if (keyword == null || keyword.isBlank()) {
            log.info("[v6 search] 키워드가 비어있어서 전체 조회로 변경");
            return getFeedRecords(pageable);
        }
        
        try {
            // 1. 모든 Record 조회
            List<Record> allRecords = recordRepository.findAllWithAssociations();
            log.info("[v6 search] 전체 Record 조회 완료: {}개", allRecords.size());
            
            if (allRecords.isEmpty()) {
                log.info("[v6 search] Record가 없어서 빈 결과 반환");
                return new PageImpl<>(List.of(), pageable, 0);
            }
            
            // 2. 모든 Record를 FeedDto로 변환
            List<FeedDto> allFeeds = allRecords.stream()
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
            
            // 3. Flask 서버 요청 데이터 생성
            FlaskSearchRequest request = FlaskSearchRequest.builder()
                .keyword(keyword)
                .feeds(allFeeds)
                .build();
            
            log.info("[v6 search] Flask 서버로 요청 전송 - 키워드: '{}', 데이터 개수: {}개", keyword, allFeeds.size());
            
            // 4. Flask 서버로 요청 전송 (v6 엔드포인트 - Hybrid 3:7)
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
                log.warn("[v6 search] Flask 서버 응답이 null이거나 결과가 없음");
                return new PageImpl<>(List.of(), pageable, 0);
            }
            
            log.info("[v6 search] Flask 서버 응답 받음: {}개 결과", response.getResults().size());
            
            // 5. Flask 서버 결과를 Spring에서 페이징 처리
            List<FeedDto> allResults = response.getResults();
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), allResults.size());
            List<FeedDto> pageContent = start > end ? List.of() : allResults.subList(start, end);
            
            return new PageImpl<>(pageContent, pageable, allResults.size());
            
        } catch (Exception e) {
            log.error("[v6 search] Flask 서버 통신 중 오류 발생", e);
            // 오류 발생 시 전체 조회로 fallback
            log.info("[v6 search] 오류로 인해 전체 조회로 fallback");
            return searchFeedRecordsV2(keyword, pageable);
        }
    }

    public Page<FeedDto> searchFeedRecordsV7(String keyword, Pageable pageable) {
        log.info("[v7 search] 시작 - keyword: '{}', pageable: {}", keyword, pageable);
        
        if (keyword == null || keyword.isBlank()) {
            log.info("[v7 search] 키워드가 비어있어서 전체 조회로 변경");
            return getFeedRecords(pageable);
        }
        
        try {
            // 1. 모든 Record 조회
            List<Record> allRecords = recordRepository.findAllWithAssociations();
            log.info("[v7 search] 전체 Record 조회 완료: {}개", allRecords.size());
            
            if (allRecords.isEmpty()) {
                log.info("[v7 search] Record가 없어서 빈 결과 반환");
                return new PageImpl<>(List.of(), pageable, 0);
            }
            
            // 2. 모든 Record를 FeedDto로 변환
            List<FeedDto> allFeeds = allRecords.stream()
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
            
            // 3. Flask 서버 요청 데이터 생성
            FlaskSearchRequest request = FlaskSearchRequest.builder()
                .keyword(keyword)
                .feeds(allFeeds)
                .build();
            
            log.info("[v7 search] Flask 서버로 요청 전송 - 키워드: '{}', 데이터 개수: {}개", keyword, allFeeds.size());
            
            // 4. Flask 서버로 요청 전송 (v7 엔드포인트 - Diversified)
            FlaskSearchResponse response = webClient.post()
                .uri(flaskServerUrl + "/search/v7")
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                    clientResponse -> {
                        log.error("[v7 search] Flask 서버 오류 응답: {}", clientResponse.statusCode());
                        return Mono.error(new RuntimeException("Flask 서버 오류: " + clientResponse.statusCode()));
                    })
                .bodyToMono(FlaskSearchResponse.class)
                .timeout(java.time.Duration.ofSeconds(30))
                .block();
            
            if (response == null || response.getResults() == null) {
                log.warn("[v7 search] Flask 서버 응답이 null이거나 결과가 없음");
                return new PageImpl<>(List.of(), pageable, 0);
            }
            
            log.info("[v7 search] Flask 서버 응답 받음: {}개 결과", response.getResults().size());
            
            // 5. Flask 서버 결과를 Spring에서 페이징 처리
            List<FeedDto> allResults = response.getResults();
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), allResults.size());
            List<FeedDto> pageContent = start > end ? List.of() : allResults.subList(start, end);
            
            return new PageImpl<>(pageContent, pageable, allResults.size());
            
        } catch (Exception e) {
            log.error("[v7 search] Flask 서버 통신 중 오류 발생", e);
            // 오류 발생 시 전체 조회로 fallback
            log.info("[v7 search] 오류로 인해 전체 조회로 fallback");
            return searchFeedRecordsV2(keyword, pageable);
        }
    }

    public Page<FeedDto> searchFeedRecordsV8(String keyword, Pageable pageable) {
        log.info("[v8 search] 시작 - keyword: '{}', pageable: {}", keyword, pageable);
        
        if (keyword == null || keyword.isBlank()) {
            log.info("[v8 search] 키워드가 비어있어서 전체 조회로 변경");
            return getFeedRecords(pageable);
        }
        
        try {
            // 1. 모든 Record 조회
            List<Record> allRecords = recordRepository.findAllWithAssociations();
            log.info("[v8 search] 전체 Record 조회 완료: {}개", allRecords.size());
            
            if (allRecords.isEmpty()) {
                log.info("[v8 search] Record가 없어서 빈 결과 반환");
                return new PageImpl<>(List.of(), pageable, 0);
            }
            
            // 2. 모든 Record를 FeedDto로 변환
            List<FeedDto> allFeeds = allRecords.stream()
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
            
            // 3. Flask 서버 요청 데이터 생성
            FlaskSearchRequest request = FlaskSearchRequest.builder()
                .keyword(keyword)
                .feeds(allFeeds)
                .build();
            
            log.info("[v8 search] Flask 서버로 요청 전송 - 키워드: '{}', 데이터 개수: {}개", keyword, allFeeds.size());
            
            // 4. Flask 서버로 요청 전송 (v7 엔드포인트 - Diversified)
            FlaskSearchResponse response = webClient.post()
                .uri(flaskServerUrl + "/search/v8")
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                    clientResponse -> {
                        log.error("[v8 search] Flask 서버 오류 응답: {}", clientResponse.statusCode());
                        return Mono.error(new RuntimeException("Flask 서버 오류: " + clientResponse.statusCode()));
                    })
                .bodyToMono(FlaskSearchResponse.class)
                .timeout(java.time.Duration.ofSeconds(30))
                .block();
            
            if (response == null || response.getResults() == null) {
                log.warn("[v8 search] Flask 서버 응답이 null이거나 결과가 없음");
                return new PageImpl<>(List.of(), pageable, 0);
            }
            
            log.info("[v8 search] Flask 서버 응답 받음: {}개 결과", response.getResults().size());
            
            // 5. Flask 서버 결과를 Spring에서 페이징 처리
            List<FeedDto> allResults = response.getResults();
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), allResults.size());
            List<FeedDto> pageContent = start > end ? List.of() : allResults.subList(start, end);
            
            return new PageImpl<>(pageContent, pageable, allResults.size());
            
        } catch (Exception e) {
            log.error("[v8 search] Flask 서버 통신 중 오류 발생", e);
            // 오류 발생 시 전체 조회로 fallback
            log.info("[v8 search] 오류로 인해 전체 조회로 fallback");
            return searchFeedRecordsV2(keyword, pageable);
        }
    }


    // 검색어 포함 횟수 기반 유사도 점수 계산
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


    // 문자열 내 검색어 등장 횟수 세기
    private int countOccurrences(String text, String keyword) {
        if (keyword.isEmpty()) return 0;
        int count = 0, idx = 0;
        while ((idx = text.indexOf(keyword, idx)) != -1) {
            count++;
            idx += keyword.length();
        }
        return count;
    }

    /**
     * OpenAI API를 통해 텍스트 임베딩 벡터(float[])를 생성합니다.
     * @param text 임베딩할 텍스트
     * @return 임베딩 벡터(float[])
     */
    private float[] getEmbeddingFromOpenAI(String text) {
        EmbeddingRequest request = EmbeddingRequest.builder()
            .model("text-embedding-ada-002")
            .input(List.of(text))
            .build();
        EmbeddingResult result = openAiService.createEmbeddings(request);
        List<Double> embedding = result.getData().get(0).getEmbedding();
        float[] arr = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            arr[i] = embedding.get(i).floatValue();
        }
        return arr;
    }

    /**
     * Record의 embeddingJson(String) 필드를 float[]로 파싱합니다.
     * @param embeddingJson JSON 배열 문자열
     * @return float[] 임베딩 벡터, 파싱 실패시 null
     */
    private float[] parseEmbedding(String embeddingJson) {
        if (embeddingJson == null || embeddingJson.isBlank()) return null;
        try {
            List<Float> list = objectMapper.readValue(embeddingJson, new TypeReference<List<Float>>() {});
            float[] arr = new float[list.size()];
            for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
            return arr;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 두 벡터의 코사인 유사도를 계산합니다.
     * @param a 벡터1
     * @param b 벡터2
     * @return 코사인 유사도(double)
     */
    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Record 엔티티를 FeedDto로 변환합니다.
     * @param record Record 엔티티
     * @return FeedDto
     */
    private FeedDto toFeedDto(Record record, Double similarityScore) {
        return FeedDto.builder()
            .userName(record.getProject().getUser().getNickname())
            .profileImageUrl(record.getProject().getUser().getProfileImageUrl())
            .projectName(record.getProject().getNickname())
            .projectId(record.getProject().getId())
            .designTitle(record.getProject().getDesign().getTitle())
            .designer(record.getProject().getDesign().getDesigner())
            .record(com.example.knittdaserver.dto.RecordResponse.from(record))
            .similarityScore(similarityScore)
            .build();
    }

    /**
     * 임베딩 유사도와 Record를 함께 보관하는 내부 클래스
     */
    private static class ScoredFeed {
        final Record record;
        final double score;
        ScoredFeed(Record record, double score) {
            this.record = record;
            this.score = score;
        }
    }

    /**
     * 모든 검색 버전(v1-v7)을 실행하고 결과를 반환
     * @param keyword 검색어
     * @param pageable 페이징 정보
     * @return 모든 버전의 검색 결과와 실행 시간
     */
    public Map<String, Object> searchAllVersions(String keyword, Pageable pageable) {
        log.info("🚀 [ALL VERSIONS] 검색 시작 - keyword: '{}', pageable: {}", keyword, pageable);
        
        Map<String, Object> results = new HashMap<>();
        Map<String, Long> executionTimes = new HashMap<>();
        Map<String, Object> versionResults = new HashMap<>();
        
        // v1: 기본 키워드 검색
        long startTime = System.currentTimeMillis();
        try {
            log.info("📋 [V1] 기본 키워드 검색 시작");
            Page<FeedDto> v1Result = searchFeedRecordsV1(keyword, pageable);
            long v1Time = System.currentTimeMillis() - startTime;
            executionTimes.put("v1", v1Time);
            versionResults.put("v1", Map.of(
                "content", v1Result.getContent(),
                "totalElements", v1Result.getTotalElements(),
                "totalPages", v1Result.getTotalPages(),
                "currentPage", v1Result.getNumber(),
                "size", v1Result.getSize()
            ));
            log.info("✅ [V1] 완료 - 실행시간: {}ms, 결과수: {}", v1Time, v1Result.getContent().size());
        } catch (Exception e) {
            log.error("❌ [V1] 실패: {}", e.getMessage());
            versionResults.put("v1", Map.of("error", e.getMessage()));
            executionTimes.put("v1", System.currentTimeMillis() - startTime);
        }
        
        // v2: OpenAI 임베딩 기반 검색
        startTime = System.currentTimeMillis();
        try {
            log.info("📋 [V2] OpenAI 임베딩 검색 시작");
            Page<FeedDto> v2Result = searchFeedRecordsV2(keyword, pageable);
            long v2Time = System.currentTimeMillis() - startTime;
            executionTimes.put("v2", v2Time);
            versionResults.put("v2", Map.of(
                "content", v2Result.getContent(),
                "totalElements", v2Result.getTotalElements(),
                "totalPages", v2Result.getTotalPages(),
                "currentPage", v2Result.getNumber(),
                "size", v2Result.getSize()
            ));
            log.info("✅ [V2] 완료 - 실행시간: {}ms, 결과수: {}", v2Time, v2Result.getContent().size());
        } catch (Exception e) {
            log.error("❌ [V2] 실패: {}", e.getMessage());
            versionResults.put("v2", Map.of("error", e.getMessage()));
            executionTimes.put("v2", System.currentTimeMillis() - startTime);
        }
        
        // v3: Flask 서버 기본 검색
        startTime = System.currentTimeMillis();
        try {
            log.info("📋 [V3] Flask 서버 기본 검색 시작");
            Page<FeedDto> v3Result = searchFeedRecordsV3(keyword, pageable);
            long v3Time = System.currentTimeMillis() - startTime;
            executionTimes.put("v3", v3Time);
            versionResults.put("v3", Map.of(
                "content", v3Result.getContent(),
                "totalElements", v3Result.getTotalElements(),
                "totalPages", v3Result.getTotalPages(),
                "currentPage", v3Result.getNumber(),
                "size", v3Result.getSize()
            ));
            log.info("✅ [V3] 완료 - 실행시간: {}ms, 결과수: {}", v3Time, v3Result.getContent().size());
        } catch (Exception e) {
            log.error("❌ [V3] 실패: {}", e.getMessage());
            versionResults.put("v3", Map.of("error", e.getMessage()));
            executionTimes.put("v3", System.currentTimeMillis() - startTime);
        }
        
        // v4: Flask 서버 Elasticsearch 검색
        startTime = System.currentTimeMillis();
        try {
            log.info("📋 [V4] Flask 서버 Elasticsearch 검색 시작");
            Page<FeedDto> v4Result = searchFeedRecordsV4(keyword, pageable);
            long v4Time = System.currentTimeMillis() - startTime;
            executionTimes.put("v4", v4Time);
            versionResults.put("v4", Map.of(
                "content", v4Result.getContent(),
                "totalElements", v4Result.getTotalElements(),
                "totalPages", v4Result.getTotalPages(),
                "currentPage", v4Result.getNumber(),
                "size", v4Result.getSize()
            ));
            log.info("✅ [V4] 완료 - 실행시간: {}ms, 결과수: {}", v4Time, v4Result.getContent().size());
        } catch (Exception e) {
            log.error("❌ [V4] 실패: {}", e.getMessage());
            versionResults.put("v4", Map.of("error", e.getMessage()));
            executionTimes.put("v4", System.currentTimeMillis() - startTime);
        }
        
        // v5: Flask 서버 Hybrid 검색 (5:5)
        startTime = System.currentTimeMillis();
        try {
            log.info("📋 [V5] Flask 서버 Hybrid 검색 (5:5) 시작");
            Page<FeedDto> v5Result = searchFeedRecordsV5(keyword, pageable);
            long v5Time = System.currentTimeMillis() - startTime;
            executionTimes.put("v5", v5Time);
            versionResults.put("v5", Map.of(
                "content", v5Result.getContent(),
                "totalElements", v5Result.getTotalElements(),
                "totalPages", v5Result.getTotalPages(),
                "currentPage", v5Result.getNumber(),
                "size", v5Result.getSize()
            ));
            log.info("✅ [V5] 완료 - 실행시간: {}ms, 결과수: {}", v5Time, v5Result.getContent().size());
        } catch (Exception e) {
            log.error("❌ [V5] 실패: {}", e.getMessage());
            versionResults.put("v5", Map.of("error", e.getMessage()));
            executionTimes.put("v5", System.currentTimeMillis() - startTime);
        }
        
        // v6: Flask 서버 Hybrid 검색 (3:7)
        startTime = System.currentTimeMillis();
        try {
            log.info("📋 [V6] Flask 서버 Hybrid 검색 (3:7) 시작");
            Page<FeedDto> v6Result = searchFeedRecordsV6(keyword, pageable);
            long v6Time = System.currentTimeMillis() - startTime;
            executionTimes.put("v6", v6Time);
            versionResults.put("v6", Map.of(
                "content", v6Result.getContent(),
                "totalElements", v6Result.getTotalElements(),
                "totalPages", v6Result.getTotalPages(),
                "currentPage", v6Result.getNumber(),
                "size", v6Result.getSize()
            ));
            log.info("✅ [V6] 완료 - 실행시간: {}ms, 결과수: {}", v6Time, v6Result.getContent().size());
        } catch (Exception e) {
            log.error("❌ [V6] 실패: {}", e.getMessage());
            versionResults.put("v6", Map.of("error", e.getMessage()));
            executionTimes.put("v6", System.currentTimeMillis() - startTime);
        }
        
        // v7: Flask 서버 다양화 검색
        startTime = System.currentTimeMillis();
        try {
            log.info("📋 [V7] Flask 서버 다양화 검색 시작");
            Page<FeedDto> v7Result = searchFeedRecordsV7(keyword, pageable);
            long v7Time = System.currentTimeMillis() - startTime;
            executionTimes.put("v7", v7Time);
            versionResults.put("v7", Map.of(
                "content", v7Result.getContent(),
                "totalElements", v7Result.getTotalElements(),
                "totalPages", v7Result.getTotalPages(),
                "currentPage", v7Result.getNumber(),
                "size", v7Result.getSize()
            ));
            log.info("✅ [V7] 완료 - 실행시간: {}ms, 결과수: {}", v7Time, v7Result.getContent().size());
        } catch (Exception e) {
            log.error("❌ [V7] 실패: {}", e.getMessage());
            versionResults.put("v7", Map.of("error", e.getMessage()));
            executionTimes.put("v7", System.currentTimeMillis() - startTime);
        }
        
        // v8: Flask 서버 v8 검색
        startTime = System.currentTimeMillis();
        try {
            log.info("📋 [V8] Flask 서버 v8 검색 시작");
            Page<FeedDto> v8Result = searchFeedRecordsV8(keyword, pageable);
            long v8Time = System.currentTimeMillis() - startTime;
            executionTimes.put("v8", v8Time);
            versionResults.put("v8", Map.of(
                "content", v8Result.getContent(),
                "totalElements", v8Result.getTotalElements(),
                "totalPages", v8Result.getTotalPages(),
                "currentPage", v8Result.getNumber(),
                "size", v8Result.getSize()
            ));
            log.info("✅ [V8] 완료 - 실행시간: {}ms, 결과수: {}", v8Time, v8Result.getContent().size());
        } catch (Exception e) {
            log.error("❌ [V8] 실패: {}", e.getMessage());
            versionResults.put("v8", Map.of("error", e.getMessage()));
            executionTimes.put("v8", System.currentTimeMillis() - startTime);
        }
        
        

        // 전체 실행 시간 계산
        long totalTime = executionTimes.values().stream().mapToLong(Long::longValue).sum();
        
        // 결과 정리
        results.put("keyword", keyword);
        results.put("pageable", Map.of(
            "page", pageable.getPageNumber(),
            "size", pageable.getPageSize(),
            "sort", pageable.getSort().toString()
        ));
        results.put("executionTimes", executionTimes);
        results.put("totalExecutionTime", totalTime);
        results.put("versions", versionResults);
        
        // 실행 시간 순으로 정렬하여 로그 출력
        log.info("📊 [ALL VERSIONS] 실행 시간 요약:");
        executionTimes.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .forEach(entry -> log.info("   {}: {}ms", entry.getKey().toUpperCase(), entry.getValue()));
        log.info("🎯 [ALL VERSIONS] 총 실행 시간: {}ms", totalTime);
        
        return results;
    }


}
