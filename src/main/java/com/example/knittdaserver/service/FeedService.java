package com.example.knittdaserver.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

import com.example.knittdaserver.dto.FeedDto;
import com.example.knittdaserver.dto.RecordResponse;
import com.example.knittdaserver.repository.RecordRepository;
import com.example.knittdaserver.entity.Record;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.embedding.EmbeddingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
@RequiredArgsConstructor
public class FeedService {
    private static final Logger log = LoggerFactory.getLogger(FeedService.class);
    private final RecordRepository recordRepository;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;

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
        List<Record> allRecords = recordRepository.findAll();
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
    public Page<FeedDto> searchFeedRecordsV2(String keyword, org.springframework.data.domain.Pageable pageable) {
        log.info("[v2 search] 시작 - keyword: '{}', pageable: {}", keyword, pageable);

        if (keyword == null || keyword.isBlank()) {
            log.info("v2 search] 키워드가 비어있어서 전체 조회로 변경");
            return getFeedRecords(pageable);
        }

        float[] queryEmbedding = getEmbeddingFromOpenAI(keyword);

        log.info("[v2 search]2계: 전체 Record 조회 시작");
        List<Record> allRecords = recordRepository.findAll();

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


}
