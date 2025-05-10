package com.example.knittdaserver.controller;

import com.example.knittdaserver.common.response.ApiResponse;
import com.example.knittdaserver.dto.CreateRecordRequest;
import com.example.knittdaserver.service.ProjectService;
import com.example.knittdaserver.service.RecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/records")
@RequiredArgsConstructor
public class RecordController {
    private final RecordService recordService;

//    @PostMapping
//    public ResponseEntity<ApiResponse<?>> createRecord(@RequestBody @Valid CreateRecordRequest request){
//        Long userId = 1L; // dummy
////        recordService.createRecord(userId, request);
//    }
}
