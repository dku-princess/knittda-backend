package com.example.knittdaserver.service;

import com.example.knittdaserver.common.metrics.BusinessMetrics;
import com.example.knittdaserver.common.metrics.ExternalCallMetrics;
import com.example.knittdaserver.common.response.ApiResponseCode;
import com.example.knittdaserver.common.response.CustomException;
import com.example.knittdaserver.dto.CreateRecordRequest;
import com.example.knittdaserver.dto.ImageOrderItem;
import com.example.knittdaserver.dto.RecordResponse;
import com.example.knittdaserver.dto.UpdateRecordRequest;
import com.example.knittdaserver.entity.*;
import com.example.knittdaserver.entity.Record;
import com.example.knittdaserver.entity.ProjectStatus;

import com.example.knittdaserver.repository.ImageRepository;
import com.example.knittdaserver.repository.ProjectRepository;
import com.example.knittdaserver.repository.RecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.embedding.EmbeddingResult;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import jakarta.transaction.Transactional;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordService {
    private final RecordRepository recordRepository;
    private final ProjectRepository projectRepository;
    private final AuthService authService;
    private final S3Service s3Service;
    private final ImageRepository imageRepository;
    private final FileUploadService fileUploadService;
    private final ProjectService projectService;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;
    private final ExternalCallMetrics externalCallMetrics;
    private final BusinessMetrics businessMetrics;
    
    @Value("${flask.server.url}")
    private String flaskServerUrl;
    
    private final WebClient webClient = WebClient.builder()
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
        .build();

    /**
     * 새로운 Record를 생성하고 임베딩을 자동으로 생성합니다.
     */
    @Transactional
    public RecordResponse createRecord(String token, CreateRecordRequest request, List<MultipartFile> files) {
        User user = authService.getUserFromJwt(token);
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new CustomException(ApiResponseCode.PROJECT_NOT_FOUND));
        projectService.validateOwnership(project, user);
        
        // 1. record에 모든 값 세팅
        Record record = Record.builder()
                .tags(request.getTags())
                .comment(request.getComment())
                .project(project)
                .recordStatus(RecordStatus.fromString(request.getRecordStatus()))
                .build();
        

        // 2. 이미지 등 추가 세팅
        if (files != null) {
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                String imageUrl = null;
                try {
                    imageUrl = fileUploadService.uploadImageAsWebp(file);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new CustomException(ApiResponseCode.IMAGE_UPLOAD_FAILED);
                }
                Image image = Image.builder()
                        .record(record)
                        .imageUrl(imageUrl)
                        .imageOrder(i + 1)
                        .build();
                record.getImages().add(image);
            }
        }
        // 3. 임베딩 생성 및 저장
        try {
            String text = buildTextFromRecord(record);
            float[] embedding = getEmbeddingFromOpenAI(text);
            String json = objectMapper.writeValueAsString(embedding);
            record.setEmbeddingJson(json);
            log.info("✅ Record 임베딩 생성 완료");
        } catch (Exception e) {
            log.error("❌ Record 임베딩 생성 실패: {}", e.getMessage());
            // 임베딩 생성 실패해도 Record는 저장
        }
        // 4. Record 저장
        Record savedRecord = recordRepository.save(record);
        project.setLastRecordAt(savedRecord.getCreatedAt());
        if (request.getRecordStatus().equals(RecordStatus.COMPLETED.toString())) {
            project.setStatus(ProjectStatus.DONE);
        }
        projectRepository.save(project);
        
        // 성공 로그
        log.info("Record 생성 완료 - recordId: {}", savedRecord.getId());

        businessMetrics.count("record.created",
                "status", project.getStatus() == ProjectStatus.DONE ? "done" : "in_progress",
                "has_image", String.valueOf(files != null && !files.isEmpty()));

        return RecordResponse.from(savedRecord);
    }

    @Transactional
    public List<RecordResponse> getAllRecords(String token) {
        User user = authService.getUserFromJwt(token);
        List<Record> records = recordRepository.findAllByUserId(user.getId());
        return records.stream().map(RecordResponse::from).toList();
    }

    @Transactional
    public List<RecordResponse> getRecordsByProjectId(Long projectId) {
        List<Record> records = recordRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        return records.stream().map(RecordResponse::from).toList();
    }


    @Transactional
    public RecordResponse getRecordById(Long recordId) {
        // User user = authService.getUserFromJwt(token);
        Record record = recordRepository.findById(recordId)
                .orElseThrow(() -> new CustomException(ApiResponseCode.RECORD_NOT_FOUND));
        // if (!record.getProject().isOwnedBy(user.getId())) {throw new CustomException(ApiResponseCode.FORBIDDEN_ACCESS);}
        return RecordResponse.from(record);
    }


    @Transactional
    public void deleteRecordById(String token, Long recordId) {
        User user = authService.getUserFromJwt(token);
        Record record = recordRepository.findById(recordId).orElseThrow(() -> new CustomException(ApiResponseCode.RECORD_NOT_FOUND));
        if (!record.getProject().isOwnedBy(user.getId())) {throw new CustomException(ApiResponseCode.FORBIDDEN_ACCESS);}
        
        // 1. S3 이미지 삭제
        record.getImages().forEach(image -> s3Service.deleteFile(image.getImageUrl()));
        
        // 2. DB에서 Record 삭제
        recordRepository.deleteById(recordId);
        log.info("[RecordService] Record 삭제 완료 - recordId: {}", recordId);
        
        // 3. Elasticsearch 동기화: Flask 서버에 삭제 요청 전송
        deleteRecordFromElasticsearch(recordId);
    }
    
    /**
     * Elasticsearch에서 Record 문서를 삭제합니다.
     * Flask 서버로 DELETE 요청을 전송하여 Elasticsearch 인덱스에서 제거합니다.
     * 
     * @param recordId 삭제할 Record ID
     */
    private void deleteRecordFromElasticsearch(Long recordId) {
        try {
            log.info("[RecordService] Elasticsearch 동기화 시작 - recordId: {}", recordId);
            
            externalCallMetrics.record("flask", "index_delete", () ->
                webClient.delete()
                    .uri(flaskServerUrl + "/index/feeds/" + recordId)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> {
                            log.warn("[RecordService] Elasticsearch 삭제 실패 - recordId: {}, status: {}",
                                recordId, clientResponse.statusCode());
                            return Mono.error(new RuntimeException("Elasticsearch 삭제 실패: " + clientResponse.statusCode()));
                        })
                    .bodyToMono(Void.class)
                    .timeout(Duration.ofSeconds(10))
                    .doOnSuccess(result -> {
                        log.info("[RecordService] ✅ Elasticsearch 동기화 완료 - recordId: {}", recordId);
                    })
                    .doOnError(error -> {
                        log.error("[RecordService] ❌ Elasticsearch 동기화 실패 - recordId: {}, error: {}",
                            recordId, error.getMessage());
                        // ES 삭제 실패해도 DB 삭제는 이미 완료되었으므로 예외를 전파하지 않음
                        // 로그만 남기고 서비스는 정상 완료로 처리
                    })
                    .block());
                
        } catch (Exception e) {
            // ES 삭제 실패는 로그만 남기고 서비스 실패로 처리하지 않음
            log.error("[RecordService] ❌ Elasticsearch 동기화 중 예외 발생 - recordId: {}, error: {}", 
                recordId, e.getMessage(), e);
        }
    }


    /**
     * Record를 업데이트하고 임베딩을 재생성합니다.
     */
    @Transactional
    public RecordResponse updateRecord(
            String token,
            UpdateRecordRequest request,
            List<Long> deleteImageIds,
            List<MultipartFile> files) {
        User user = authService.getUserFromJwt(token);
        Record record = recordRepository.findById(request.getRecordId())
                .orElseThrow(() -> new CustomException(ApiResponseCode.RECORD_NOT_FOUND));
        // 프로젝트 소유 검증
        if (!record.getProject().isOwnedBy(user.getId())) {
            throw new CustomException(ApiResponseCode.FORBIDDEN_ACCESS);
        }
        // 1. 삭제할 사진 처리 (deleteImageIds 우선)
        if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
            List<Image> imagesToDelete = imageRepository.findAllById(deleteImageIds);
            for (Image image : imagesToDelete) {
                s3Service.deleteFile(image.getImageUrl());
                imageRepository.delete(image);
            }
            record.getImages().removeAll(imagesToDelete);
        }

        // 2. 신규 파일 업로드 (imageOrder는 아직 미정 — 3단계에서 재부여)
        List<Image> uploadedImages = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                String imageUrl;
                try {
                    imageUrl = fileUploadService.uploadImageAsWebp(file);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new CustomException(ApiResponseCode.IMAGE_UPLOAD_FAILED);
                }
                uploadedImages.add(Image.builder()
                        .record(record)
                        .imageUrl(imageUrl)
                        .build());
            }
        }

        // 3. 최종 순서(imageOrder) 재부여
        applyImageOrder(record, request.getImages(), uploadedImages);
        // Record 업데이트
        record.updateFromRequest(request);
        // 임베딩 재생성
        try {
            String text = buildTextFromRecord(record);
            float[] embedding = getEmbeddingFromOpenAI(text);
            String json = objectMapper.writeValueAsString(embedding);
            record.setEmbeddingJson(json);
            log.info("✅ Record 임베딩 재생성 완료");
        } catch (Exception e) {
            log.error("❌ Record 임베딩 재생성 실패: {}", e.getMessage());
            // 임베딩 생성 실패해도 Record는 저장
        }
        return RecordResponse.from(recordRepository.save(record));
    }

    /**
     * 수정 시 이미지의 최종 표시 순서(imageOrder, 1-base)를 재부여한다. (제안 A)
     *
     * <p>{@code orderItems}(record.images)의 배열 순서가 곧 최종 표시 순서이며, 서버가 그 위치로
     * imageOrder를 통째로 재계산한다(증분 갱신이 아니라 전체 재부여 → gap·중복 원천 차단).
     *
     * <ul>
     *   <li>orderItems == null : 순서 미지정. 기존 이미지 순서를 유지하고 신규 파일만 뒤에 append.</li>
     *   <li>orderItems == []   : 위와 동일(빈 배열은 전체 삭제가 아님).</li>
     *   <li>그 외              : orderItems 순서대로 existing/new 를 배치. 배열에 빠진 신규는 방어적으로 tail append.</li>
     * </ul>
     *
     * @param record        대상 Record (삭제 반영 후 상태)
     * @param orderItems    클라가 보낸 최종 순서 배열 (record.images), null 허용
     * @param uploadedImages 이번 요청에서 업로드된 신규 Image들 (files 인덱스 순서, 아직 record.images 미포함)
     */
    private void applyImageOrder(Record record, List<ImageOrderItem> orderItems, List<Image> uploadedImages) {
        // 순서 미지정: 기존 순서 유지 + 신규는 max+1 부터 뒤에 append
        if (orderItems == null || orderItems.isEmpty()) {
            int next = record.getImages().stream()
                    .map(Image::getImageOrder)
                    .filter(java.util.Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(0) + 1;
            for (Image img : uploadedImages) {
                img.setImageOrder(next++);
                record.getImages().add(img);
            }
            return;
        }

        // 기존 이미지 id -> 엔티티 매핑 (삭제 반영 후 남아있는 것만)
        java.util.Map<Long, Image> existingById = new java.util.HashMap<>();
        for (Image img : record.getImages()) {
            existingById.put(img.getId(), img);
        }

        java.util.Set<Image> placed = new java.util.HashSet<>();
        int pos = 1;
        for (ImageOrderItem item : orderItems) {
            Image target;
            if (item.isNew()) {
                Integer idx = item.getIndex();
                if (idx == null || idx < 0 || idx >= uploadedImages.size()) {
                    throw new CustomException(ApiResponseCode.IMAGE_ORDER_INVALID);
                }
                target = uploadedImages.get(idx);
            } else if (item.isExisting()) {
                target = existingById.get(item.getId());
                if (target == null) {
                    // 삭제되었거나 이 Record 소유가 아닌 id → 무시(delete 우선 원칙)
                    continue;
                }
            } else {
                throw new CustomException(ApiResponseCode.IMAGE_ORDER_INVALID);
            }
            target.setImageOrder(pos++);
            placed.add(target);
        }

        // 배열에 실리지 않은 신규 이미지는 방어적으로 뒤에 append
        for (Image img : uploadedImages) {
            if (!placed.contains(img)) {
                img.setImageOrder(pos++);
            }
            if (!record.getImages().contains(img)) {
                record.getImages().add(img);
            }
        }
    }

    /**
     * OpenAI API를 사용하여 텍스트의 임베딩을 생성합니다.
     * @param text 임베딩을 생성할 텍스트
     * @return float임베딩 벡터
     */
    private float[] getEmbeddingFromOpenAI(String text) {
        EmbeddingRequest request = EmbeddingRequest.builder()
            .model("text-embedding-ada-002")
            .input(List.of(text))
            .build();
        
        EmbeddingResult result = externalCallMetrics.record("openai", "embedding",
                () -> openAiService.createEmbeddings(request));
        List<Double> embeddingList = result.getData().get(0).getEmbedding();
        
        float[] embedding = new float[embeddingList.size()];
        for (int i = 0; i < embeddingList.size(); i++) {
            embedding[i] = embeddingList.get(i).floatValue();
        }
        
        return embedding;
    }

    /**
     * Record의 모든 관련 정보를 조합하여 임베딩용 텍스트를 생성합니다.
     * @param record 임베딩 텍스트를 생성할 Record
     * @return 조합된 텍스트
     */
    private String buildTextFromRecord(Record record) {
        StringBuilder sb = new StringBuilder();
        
        // 프로젝트 정보
        Project project = record.getProject();
        if (project != null) {
            if (project.getNickname() != null) {
                sb.append(project.getNickname()).append(" ");
            }
            
            // 디자인 정보
            Design design = project.getDesign();
            if (design != null) {
                if (design.getTitle() != null) {
                    sb.append(design.getTitle()).append(" ");
                }
                if (design.getDesigner() != null) {
                    sb.append(design.getDesigner()).append(" ");
                }
            }
        }
        
        // 댓글
        if (record.getComment() != null && !record.getComment().isBlank()) {
            sb.append(record.getComment()).append(" ");
        }
        
        // 태그
        if (record.getTags() != null) {
            for (String tag : record.getTags()) {
                if (tag != null && !tag.isBlank()) {
                    sb.append(tag).append(" ");
                }
            }
        }
        
        return sb.toString().trim();
    }

}
