package com.example.knittdaserver.service;

import com.example.knittdaserver.common.response.ApiResponseCode;
import com.example.knittdaserver.common.response.CustomException;
import com.example.knittdaserver.dto.CreateRecordRequest;
import com.example.knittdaserver.dto.RecordResponse;
import com.example.knittdaserver.dto.UpdateRecordRequest;
import com.example.knittdaserver.entity.Image;
import com.example.knittdaserver.entity.Project;
import com.example.knittdaserver.entity.Record;
import com.example.knittdaserver.entity.User;
import com.example.knittdaserver.repository.ImageRepository;
import com.example.knittdaserver.repository.ProjectRepository;
import com.example.knittdaserver.repository.RecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecordService {
    private final RecordRepository recordRepository;
    private final ProjectRepository projectRepository;
    private final AuthService authService;
    private final S3Service s3Service;
    private final ImageRepository imageRepository;


    public RecordResponse createRecord(String token, CreateRecordRequest request, List<MultipartFile> files) {
        User user = authService.getUserFromJwt(token);
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new CustomException(ApiResponseCode.PROJECT_NOT_FOUND));
        Record record = Record.builder()
                .tags(request.getTags())
                .comment(request.getComment())
                .project(project)
                .recordStatus(request.getRecordStatus())
                .build();

        if (files != null) {
            for (int i = 0; i < files.size(); i++) {
                String imageUrl = s3Service.uploadFile(files.get(i));
                Image image = Image.builder()
                        .record(record)
                        .imageUrl(imageUrl)
                        .imageOrder((long) (i + 1))
                        .build();
                record.addImage(image);
            }
        }


        return RecordResponse.from(recordRepository.save(record));
    }

    public List<RecordResponse> getAllRecords(String token) {
        User user = authService.getUserFromJwt(token);
        List<Record> records = recordRepository.findAllByUserId(user.getId());
        return records.stream().map(RecordResponse::from).toList();
    }

    public List<RecordResponse> getRecordsByProjectId(String token, Long projectId) {
        User user = authService.getUserFromJwt(token);
        // project 검증
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ApiResponseCode.PROJECT_NOT_FOUND));
        if (!project.isOwnedBy(user.getId())){throw new CustomException(ApiResponseCode.FORBIDDEN_ACCESS);}
        List<Record> records = recordRepository.findByProjectId(projectId);
        return records.stream().map(RecordResponse::from).toList();
    }


    public RecordResponse getRecordById(String token, Long recordId) {
        User user = authService.getUserFromJwt(token);
        Record record = recordRepository.findById(recordId)
                .orElseThrow(() -> new CustomException(ApiResponseCode.RECORD_NOT_FOUND));
        if (!record.getProject().isOwnedBy(user.getId())) {throw new CustomException(ApiResponseCode.FORBIDDEN_ACCESS);}
        return RecordResponse.from(record);
    }


    public void deleteRecordById(String token, Long recordId) {
        User user = authService.getUserFromJwt(token);
        Record record = recordRepository.findById(recordId).orElseThrow(() -> new CustomException(ApiResponseCode.RECORD_NOT_FOUND));
        if (!record.getProject().isOwnedBy(user.getId())) {throw new CustomException(ApiResponseCode.FORBIDDEN_ACCESS);}
        record.getImages().forEach(image -> s3Service.deleteFile(image.getImageUrl()));
        recordRepository.deleteById(recordId);
    }


    public RecordResponse updateRecord(String token, UpdateRecordRequest request,
                                       List<Long> deleteImageIds, List<MultipartFile> files) {
        User user = authService.getUserFromJwt(token);
        Record record = recordRepository.findById(request.getRecordId())
                .orElseThrow(() -> new CustomException(ApiResponseCode.RECORD_NOT_FOUND));
        // 프로젝트 소유 검증
        if (!record.getProject().isOwnedBy(user.getId())) {
            throw new CustomException(ApiResponseCode.FORBIDDEN_ACCESS);
        }

        // 프로젝트를 수정할 경우, 프로젝트 소유 검증
        if (request.getProject() != null && request.getProject().isOwnedBy(user.getId())){
            throw new CustomException(ApiResponseCode.FORBIDDEN_ACCESS);
        }

        // 삭제할 사진이 있을 경우
        if (deleteImageIds != null) {
            List<Image> imagesToDelete = imageRepository.findAllById(deleteImageIds);
            for (Image image : imagesToDelete) {
                s3Service.deleteFile(image.getImageUrl());
                imageRepository.delete(image);
            }
        }

        // 추가할 사진이 있을 경우
        if (files != null) {
            for (int i = 0; i < files.size(); i++) {
                String imageUrl = s3Service.uploadFile(files.get(i));
                Image image = Image.builder()
                        .record(record)
                        .imageUrl(imageUrl)
                        .imageOrder((long) (i + 1))
                        .build();
                record.addImage(image);
            }
        }
        record.updateFromRequest(request);
        return RecordResponse.from(recordRepository.save(record));
    }

}
