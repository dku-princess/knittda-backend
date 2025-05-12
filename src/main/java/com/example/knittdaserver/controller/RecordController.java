package com.example.knittdaserver.controller;

import com.example.knittdaserver.common.response.ApiResponse;
import com.example.knittdaserver.dto.CreateRecordRequest;
import com.example.knittdaserver.dto.RecordResponse;
import com.example.knittdaserver.dto.UpdateRecordRequest;
import com.example.knittdaserver.entity.Record;
import com.example.knittdaserver.service.ProjectService;
import com.example.knittdaserver.service.RecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/records")
@RequiredArgsConstructor
public class RecordController {
    private final RecordService recordService;

    @PostMapping("/")
    public ResponseEntity<ApiResponse<RecordResponse>> createRecord(
            @RequestHeader(name = "Authorization") String token,
            @Valid @RequestBody CreateRecordRequest request
    )  {
        RecordResponse record = recordService.createRecord(token, request);
        return ResponseEntity.ok(ApiResponse.success(record));
    }

    @GetMapping("/")
    public ResponseEntity<ApiResponse<List<RecordResponse>>> getAllRecords(
            @RequestHeader(name = "Authorization") String token
    ){
        List<RecordResponse> records = recordService.getAllRecords(token);
        return ResponseEntity.ok(ApiResponse.success(records));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<List<RecordResponse>>> getRecordsByProjectId(
            @RequestHeader(name = "Authorization") String token,
            @PathVariable Long projectId
    ){
        List<RecordResponse> records = recordService.getRecordsByProjectId(token, projectId);
        return ResponseEntity.ok(ApiResponse.success(records));
    }

    @GetMapping("/{recordId}")
    public ResponseEntity<ApiResponse<RecordResponse>> getRecordById(
            @RequestHeader(name = "Authorization") String token,
            @PathVariable Long recordId
    ){
        RecordResponse record = recordService.getRecordById(token, recordId);
        return ResponseEntity.ok(ApiResponse.success(record));
    }

    @PutMapping("/")
    public ResponseEntity<ApiResponse<RecordResponse>> updateRecord(
            @RequestHeader(name = "Authorization") String token,
            @Valid @RequestBody UpdateRecordRequest request
    ){
        RecordResponse response = recordService.updateRecord(token, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{recordId}")
    public ResponseEntity<ApiResponse<Void>> deleteRecordById(
            @RequestHeader(name = "Authorization") String token,
            @PathVariable Long recordId
    ){
        recordService.deleteRecordById(token, recordId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

