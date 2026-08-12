package com.example.knittdaserver.controller;

import com.example.knittdaserver.common.response.ApiResponse;
import com.example.knittdaserver.dto.CreateDesignRequest;
import com.example.knittdaserver.dto.DesignDto;
import com.example.knittdaserver.service.DesignService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/api/v1/designs")
@RequiredArgsConstructor
public class DesignController {


    // private final DesignQueryService designQueryService;
    private final DesignService designService;
    // private final ProjectService projectService;

    @Operation(summary = "도안 생성", description = "사용자 개인 도안을 생성합니다.")
    @PostMapping("/")
    public ResponseEntity<ApiResponse<DesignDto>> createDesign(@RequestBody CreateDesignRequest request) {
        DesignDto design = designService.createDesign(request);
        return ResponseEntity.ok(ApiResponse.success(design));
    }

    // @Operation(summary = "도안 검색 조회", description = "도안을 키워드 기반으로 검색합니다.")
    // @GetMapping("/search")
    // public ResponseEntity<ApiResponse<List<DesignDto>>> search(@RequestParam(required = false) String keyword) {
    //     List<DesignDto> result = designQueryService.search(keyword);
    //     return ResponseEntity.ok(ApiResponse.success(result));
    // }

    // @Operation(summary = "도안 간단 검색 조회", description = "도안을 키워드 기반으로 검색하고, 간단 정보만 반환합니다.")
    // @GetMapping("/search/summary")
    // public ResponseEntity<ApiResponse<List<DesignSummaryResponse>>> searchSummary(@RequestParam(required = false) String keyword) {
    //     List<DesignDto> designDtos = designQueryService.search(keyword);
    //     List<DesignSummaryResponse> result = designDtos.stream()
    //         .map(DesignSummaryResponse::from)
    //         .toList();
    //     return ResponseEntity.ok(ApiResponse.success(result));
    // }

    // @Operation(summary = "공식 도안 조회", description = "공식 도안을 조회합니다.")
    // @GetMapping("/public")
    // public ResponseEntity<ApiResponse<List<DesignSummaryResponse>>> getPublicDesigns() {
    //     List<DesignSummaryResponse> result = designService.getPublicDesigns();
    //     return ResponseEntity.ok(ApiResponse.success(result));
    // }

    // @Operation(summary = "공식 도안 상세 조회", description = "공식 도안을 상세 조회합니다.")
    // @GetMapping("/public/{designId}")
    // public ResponseEntity<ApiResponse<DesignResponse>> getPublicDesignDetail(@PathVariable Long designId) {
    //     DesignResponse result = designService.getPublicDesignDetail(designId);
    //     return ResponseEntity.ok(ApiResponse.success(result));
    // }

    // @Operation(summary = "도안 별 프로젝트 조회", description = "도안 별 프로젝트를 조회합니다.")
    // @GetMapping("/public/{designId}/projects")
    // public ResponseEntity<ApiResponse<List<ProjectPreviewResponse>>> getPublicDesignProjects(@PathVariable Long designId) {
    //     List<ProjectPreviewResponse> result = projectService.getDesignProjects(designId);
    //     return ResponseEntity.ok(ApiResponse.success(result));
    // }

}
