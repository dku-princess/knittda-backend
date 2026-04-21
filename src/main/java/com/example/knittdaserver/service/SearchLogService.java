package com.example.knittdaserver.service;

import com.example.knittdaserver.dto.SearchClickLogRequest;
import com.example.knittdaserver.entity.Record;
import com.example.knittdaserver.entity.SearchClickLog;
import com.example.knittdaserver.entity.SearchEventLog;
import com.example.knittdaserver.repository.RecordRepository;
import com.example.knittdaserver.repository.SearchClickLogRepository;
import com.example.knittdaserver.repository.SearchEventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchLogService {
    
    private final SearchEventLogRepository searchEventLogRepository;
    private final SearchClickLogRepository searchClickLogRepository;
    private final RecordRepository recordRepository;
    
    /**
     * 검색 이벤트 로그를 비동기로 저장합니다.
     * 로그 저장 실패는 검색 결과 반환을 막지 않습니다.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSearchEventLog(String searchId, Long userId, String keyword, String searchVersion) {
        try {
            log.debug("[SearchLog] 검색 이벤트 로그 저장 시작 - searchId: {}, keyword: {}, version: {}", 
                searchId, keyword, searchVersion);
            
            SearchEventLog eventLog = SearchEventLog.builder()
                .searchId(searchId)
                .userId(userId)
                .keyword(keyword)
                .searchVersion(searchVersion)
                .build();
            
            searchEventLogRepository.save(eventLog);
            log.info("[SearchLog] ✅ 검색 이벤트 로그 저장 완료 - searchId: {}, keyword: '{}', version: {}, userId: {}", 
                searchId, keyword, searchVersion, userId);
        } catch (Exception e) {
            log.error("[SearchLog] ❌ 검색 이벤트 로그 저장 실패 - searchId: {}, keyword: '{}', error: {}", 
                searchId, keyword, e.getMessage(), e);
            // 로그 저장 실패는 예외를 전파하지 않음 (UX 보호)
        }
    }
    
    /**
     * 검색 결과 클릭 로그를 비동기로 저장합니다.
     * 로그 저장 실패는 서비스 실패로 간주하지 않습니다.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSearchClickLog(SearchClickLogRequest request, Long userId, String searchVersion) {
        try {
            log.debug("[SearchLog] 클릭 로그 저장 시작 - searchId: {}, recordId: {}, clickRank: {}", 
                request.getSearchId(), request.getRecordId(), request.getClickRank());
            
            // 필수 필드 유효성 확인
            if (request.getClickRank() == null) {
                log.warn("[SearchLog] ⚠️ clickRank가 null입니다 - searchId: {}, recordId: {}", 
                    request.getSearchId(), request.getRecordId());
                return;
            }
            
            if (request.getPage() == null) {
                log.warn("[SearchLog] ⚠️ page가 null입니다 - searchId: {}, recordId: {}", 
                    request.getSearchId(), request.getRecordId());
                return;
            }
            
            // recordId 유효성 확인 및 상세 디버깅
            Long requestedRecordId = request.getRecordId();
            boolean recordExists = recordRepository.existsById(requestedRecordId);
            
            if (!recordExists) {
                log.warn("[SearchLog] ⚠️ 유효하지 않은 recordId 발견!");
                log.warn("[SearchLog] ⚠️ 클라이언트가 보낸 recordId: {}", requestedRecordId);
                log.warn("[SearchLog] ⚠️ searchId: {}, keyword: '{}', clickRank: {}, page: {}", 
                    request.getSearchId(), request.getKeyword(), request.getClickRank(), request.getPage());
                
                // DB에 실제 존재하는 Record ID 샘플 조회 (디버깅용)
                try {
                    List<Record> sampleRecords = recordRepository.findAll().stream()
                        .limit(10)
                        .collect(Collectors.toList());
                    List<Long> sampleRecordIds = sampleRecords.stream()
                        .map(Record::getId)
                        .collect(Collectors.toList());
                    log.warn("[SearchLog] ⚠️ DB에 존재하는 Record ID 샘플 (최근 10개): {}", sampleRecordIds);
                } catch (Exception e) {
                    log.warn("[SearchLog] ⚠️ Record 샘플 조회 실패: {}", e.getMessage());
                }
                
                return;
            }
            
            SearchClickLog clickLog = SearchClickLog.builder()
                .searchId(request.getSearchId())
                .userId(userId)
                .keyword(request.getKeyword())
                .recordId(request.getRecordId())
                .clickRank(request.getClickRank())
                .page(request.getPage())
                .searchVersion(searchVersion)
                .build();
            
            searchClickLogRepository.save(clickLog);
            log.info("[SearchLog] ✅ 클릭 로그 저장 완료 - searchId: {}, recordId: {}, clickRank: {}, page: {}, userId: {}", 
                request.getSearchId(), request.getRecordId(), request.getClickRank(), request.getPage(), userId);
        } catch (Exception e) {
            log.error("[SearchLog] ❌ 클릭 로그 저장 실패 - searchId: {}, recordId: {}, error: {}", 
                request.getSearchId(), request.getRecordId(), e.getMessage(), e);
            // 로그 저장 실패는 예외를 전파하지 않음 (UX 보호)
        }
    }
    
    /**
     * searchId 존재 여부 확인
     */
    public boolean existsSearchId(String searchId) {
        return searchEventLogRepository.existsBySearchId(searchId);
    }
    
    /**
     * searchId로 검색 버전 조회
     */
    public String getSearchVersionBySearchId(String searchId) {
        return searchEventLogRepository.findBySearchId(searchId)
            .map(SearchEventLog::getSearchVersion)
            .orElse(null);
    }
    
    /**
     * UUID 기반 searchId 생성
     */
    public String generateSearchId() {
        return UUID.randomUUID().toString();
    }
}

