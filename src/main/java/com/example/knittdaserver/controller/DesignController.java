package com.example.knittdaserver.controller;

import com.example.knittdaserver.common.response.ApiResponse;
import com.example.knittdaserver.dto.CreateDesignRequest;
import com.example.knittdaserver.dto.DesignDto;
import com.example.knittdaserver.service.DesignQueryService;
import com.example.knittdaserver.service.DesignService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/designs")
@RequiredArgsConstructor
public class DesignController {


    private final DesignQueryService designQueryService;
    private final DesignService designService;

    @Operation(summary = "도안 생성", description = "사용자 개인 도안을 생성합니다.")
    @PostMapping("/")
    public ResponseEntity<ApiResponse<DesignDto>> createDesign(@RequestBody CreateDesignRequest request) {
        DesignDto design = designService.createDesign(request);
        return ResponseEntity.ok(ApiResponse.success(design));
    }

    @Operation(summary = "도안 검색 조회", description = "도안을 키워드 기반으로 검색합니다.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<DesignDto>>> search(@RequestParam(required = false) String keyword) {
        List<DesignDto> result = designQueryService.search(keyword);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
