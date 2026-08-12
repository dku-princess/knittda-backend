package com.example.knittdaserver.controller;

import com.example.knittdaserver.common.response.ApiResponse;
import com.example.knittdaserver.dto.CreateRecordRequest;
import com.example.knittdaserver.dto.RecordResponse;
import com.example.knittdaserver.dto.UpdateRecordRequest;
import com.example.knittdaserver.service.RecordService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/records")
@RequiredArgsConstructor
@Tag(name = "Record API", description = "Record 관련 API")
public class RecordController {
    private final RecordService recordService;

    @Autowired
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/", consumes = {"multipart/form-data"})
    @Operation(summary = "새로운 Record 생성", description = "새로운 Record를 생성합니다.")
    public ResponseEntity<ApiResponse<RecordResponse>> createRecord(
            @RequestHeader(name = "Authorization") String token,
            @RequestPart(value = "record") String recordJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    )  {
        CreateRecordRequest request;
        try {
            request = objectMapper.readValue(recordJson, CreateRecordRequest.class);
            log.info(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON format for 'record'", e);
        }

        RecordResponse record = recordService.createRecord(token, request, files);
        return ResponseEntity.ok(ApiResponse.success(record));
    }


    @Operation(summary = "개인 Record 조회", description = "개인의 모든 Record를 조회합니다.")
    @GetMapping("/")
    public ResponseEntity<ApiResponse<List<RecordResponse>>> getAllRecords(
            @RequestHeader(name = "Authorization") String token
    ){
        List<RecordResponse> records = recordService.getAllRecords(token);
        return ResponseEntity.ok(ApiResponse.success(records));
    }

    @Operation(summary = "프로젝트별 Record 조회", description = "프로젝트 ID를 기반으로 Record를 조회합니다.")
    @GetMapping("/projects/{projectId}")
    public ResponseEntity<ApiResponse<List<RecordResponse>>> getMyRecordsByProjectId(
            @PathVariable Long projectId
    ){
        List<RecordResponse> records = recordService.getRecordsByProjectId(projectId);
        return ResponseEntity.ok(ApiResponse.success(records));
    }


    @Operation(summary = "Record 상세 조회", description = "Record ID를 기반으로 Record를 조회합니다.")
    @GetMapping("/{recordId}")
    public ResponseEntity<ApiResponse<RecordResponse>> getRecordById(
            @PathVariable Long recordId
    ){
        RecordResponse record = recordService.getRecordById(recordId);
        return ResponseEntity.ok(ApiResponse.success(record));
    }

    @Operation(summary = "Record 업데이트", description = "Record 정보를 업데이트합니다.")
    @PutMapping(value = "/", consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<RecordResponse>> updateRecord(
            @RequestHeader(name = "Authorization") String token,
            @RequestPart(value = "record") String updateRecordJson,
            @RequestPart(value = "deleteImageIds", required = false) String deleteImageIdsJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ){
        UpdateRecordRequest request;
        List<Long> deleteImageIds = new ArrayList<>();  // 빈 리스트로 초기화

        try {
            // record JSON 파싱
            request = objectMapper.readValue(updateRecordJson, UpdateRecordRequest.class);

            // deleteImageIds JSON 파싱
            if (deleteImageIdsJson != null && !deleteImageIdsJson.isEmpty()) {
                log.info("Raw deleteImageIdsJson: {}", deleteImageIdsJson);

                try {
                    deleteImageIds = objectMapper.readValue(
                            deleteImageIdsJson,
                            new TypeReference<ArrayList<Long>>() {}
                    );
                    log.info("Parsed deleteImageIds: {}", objectMapper.writeValueAsString(deleteImageIds));
                } catch (JsonProcessingException e) {
                    log.warn("Invalid JSON format for 'deleteImageIds': {}", deleteImageIdsJson, e);
                }
            }

        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON format for 'record'", e);
        }

        RecordResponse response = recordService.updateRecord(token, request, deleteImageIds, files);
        return ResponseEntity.ok(ApiResponse.success(response));
    }


    @Operation(summary = "Record 삭제", description = "Record ID를 기반으로 Record를 삭제합니다.")
    @DeleteMapping("/{recordId}")
    public ResponseEntity<ApiResponse<Void>> deleteRecordById(
            @RequestHeader(name = "Authorization") String token,
            @PathVariable Long recordId
    ){
        recordService.deleteRecordById(token, recordId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

