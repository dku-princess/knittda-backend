package com.example.knittdaserver.service;

import com.example.knittdaserver.common.response.ApiResponseCode;
import com.example.knittdaserver.common.response.CustomException;
import com.example.knittdaserver.dto.CreateRecordRequest;
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

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
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

    /**
     * 새로운 Record를 생성하고 임베딩을 자동으로 생성합니다.
     */
    @Transactional
    public RecordResponse createRecord(String token, CreateRecordRequest request, List<MultipartFile> files) {
        User user = authService.getUserFromJwt(token);
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new CustomException(ApiResponseCode.PROJECT_NOT_FOUND));
        projectService.validateOwnership(project, user);


        log.info("[250805 수정] Record 생성 시작 - question: {}, comment: {}, user: {}, commentLength: {}, imageCount: {}", 
                request.getQuestion(), 
                request.getComment(),
                user.getNickname(),
                request.getComment() != null ? request.getComment().length() : 0, 
                files != null ? files.size() : 0);
        
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
        record.getImages().forEach(image -> s3Service.deleteFile(image.getImageUrl()));
        recordRepository.deleteById(recordId);
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
        // 삭제할 사진이 있을 경우
        if (deleteImageIds != null) {
            List<Image> imagesToDelete = imageRepository.findAllById(deleteImageIds);
            for (Image image : imagesToDelete) {
                s3Service.deleteFile(image.getImageUrl());
                imageRepository.delete(image);
            }
            record.getImages().removeAll(imagesToDelete);
        }
        // 추가할 사진이 있을 경우
        if (files != null) {
            log.info(files.toString());
            for (int i = 0; i < files.size(); i++) {
                String imageUrl = null;
                try {
                    imageUrl = fileUploadService.uploadImageAsWebp(files.get(i));
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
     * OpenAI API를 사용하여 텍스트의 임베딩을 생성합니다.
     * @param text 임베딩을 생성할 텍스트
     * @return float임베딩 벡터
     */
    private float[] getEmbeddingFromOpenAI(String text) {
        EmbeddingRequest request = EmbeddingRequest.builder()
            .model("text-embedding-ada-002")
            .input(List.of(text))
            .build();
        
        EmbeddingResult result = openAiService.createEmbeddings(request);
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
