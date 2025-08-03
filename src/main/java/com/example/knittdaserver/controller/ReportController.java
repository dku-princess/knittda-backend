package com.example.knittdaserver.controller;

import com.example.knittdaserver.common.response.ApiResponse;

import com.example.knittdaserver.dto.ReportResponse;
import com.example.knittdaserver.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    @PostMapping(value = "/")
    public ResponseEntity<ApiResponse<ReportResponse>> createReport(
            @RequestHeader(name = "Authorization") String token
    ){
        return ResponseEntity.ok(ApiResponse.success(reportService.createReport(token)));
    }

}
