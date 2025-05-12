package com.example.knittdaserver.service;

import com.example.knittdaserver.common.response.ApiResponseCode;
import com.example.knittdaserver.common.response.CustomException;
import com.example.knittdaserver.dto.CreateRecordRequest;
import com.example.knittdaserver.dto.RecordResponse;
import com.example.knittdaserver.dto.UpdateRecordRequest;
import com.example.knittdaserver.entity.Project;
import com.example.knittdaserver.entity.Record;
import com.example.knittdaserver.entity.User;
import com.example.knittdaserver.repository.ProjectRepository;
import com.example.knittdaserver.repository.RecordRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecordService {
    private final RecordRepository recordRepository;
    private final ProjectRepository projectRepository;
    private final AuthService authService;



    public RecordResponse createRecord(String token, CreateRecordRequest request) {
        authService.getUserFromJwt(token);
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new CustomException(ApiResponseCode.PROJECT_NOT_FOUND));
        Record record = Record.builder()
                .tags(request.getTags())
                .comment(request.getComment())
                .project(project)
                .recordStatus(request.getRecordStatus())
                .build();
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

        recordRepository.deleteById(recordId);
    }

    // 프로젝트 delete 시
    public void deleteRecordsByProjectId(String token, Long projectId) {
        User user = authService.getUserFromJwt(token);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ApiResponseCode.PROJECT_NOT_FOUND));

        // project 소유 검증
        if (!project.isOwnedBy(user.getId())) {throw new CustomException(ApiResponseCode.FORBIDDEN_ACCESS);}

        List<Record> records = recordRepository.findByProjectId(projectId);
        recordRepository.deleteAll(records);
    }


    public RecordResponse updateRecord(String token, UpdateRecordRequest request) {
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
        record.updateFromRequest(request);
        return RecordResponse.from(recordRepository.save(record));
    }

}
