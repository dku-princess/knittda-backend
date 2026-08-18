package com.example.knittdaserver.controller;

import com.example.knittdaserver.common.response.ApiResponse;
import com.example.knittdaserver.dto.CreateProjectRequest;
import com.example.knittdaserver.dto.DefaultProjectThumbnailResponse;
import com.example.knittdaserver.dto.ProjectArticlePreviewPayload;
import com.example.knittdaserver.dto.ProjectDto;
import com.example.knittdaserver.dto.ProjectPreviewResponse;
import com.example.knittdaserver.dto.UpdateProjectRequest;
import com.example.knittdaserver.service.ProjectService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.sentry.Sentry;
import jakarta.annotation.PostConstruct;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Project", description = "프로젝트 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final MeterRegistry meterRegistry;
    @Autowired
    private ObjectMapper objectMapper;

    // v1 deprecated endpoint 호출 카운터 (Micrometer)
    private Counter v1PreviewsCounter;

    @PostConstruct
    void initMetrics() {
        // v2 와 동일한 메트릭 이름으로 통일하고 version 라벨로 분리 — PromQL group by 용이.
        this.v1PreviewsCounter = Counter.builder("api.previews.calls")
                .description("project previews endpoint invocations")
                .tag("endpoint", "GET /api/v1/projects/previews")
                .tag("version", "v1")
                .tag("deprecated", "true")
                .register(meterRegistry);
    }

    @Operation(summary = "프로젝트 생성", description = "새로운 프로젝트를 생성합니다.")
    @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE )
    public ResponseEntity<ApiResponse<ProjectDto>> createProject(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization") String token,
            @Parameter(
                    description = "프로젝트 정보 JSON",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateProjectRequest.class)
                    )
            )
            @RequestPart(value = "project") String projectJson,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {

        CreateProjectRequest request;
        try {
            request = objectMapper.readValue(projectJson, CreateProjectRequest.class);
        }catch (JsonProcessingException e){
            throw new IllegalArgumentException("Invalid JSON format for 'record'", e);
        }

        ProjectDto projectDto = projectService.createProject(token, request, file);
        return ResponseEntity.ok(ApiResponse.success(projectDto));
    }
    
    // DEPRECATED: 2026-06-19 v2 출시와 함께 레거시화. 제거 검토 목표 2026-12-19 (+6개월).
    // 제거 조건(OR): (A) v2 출시 후 6개월 경과, (B) 구버전 MAU < 1%, (C) v1 일평균 호출 < 500.
    // 호출량은 Sentry 트랜잭션 태그 api.deprecated:true 로 측정.
    @Operation(summary = "[v1, 레거시] 프로젝트 미리보기 조회",
            description = "구버전 클라이언트 호환용. 페이지네이션 없이 최신 순으로 최대 " +
                    "100건까지만 반환합니다. 신규 클라이언트는 GET /api/v2/projects/previews 사용.")
    @GetMapping("/previews")
    public ResponseEntity<ApiResponse<List<ProjectPreviewResponse>>> getProjectPreviews(
            @RequestHeader(name = "User-Agent", required = false) String userAgent,
            @RequestHeader(name = "X-App-Version", required = false) String appVersion
    ){
        // [DEPRECATION TRACKING]
        // 1) Micrometer Counter — 샘플링 없는 정확한 카운트 (Prometheus/Grafana 에서 조회)
        v1PreviewsCounter.increment();
        // 2) Sentry 태깅 — Discover/Insights 에서 호출 컨텍스트(트레이스/사용자) 분석용
        Sentry.configureScope(scope -> {
            scope.setTag("api.deprecated", "true");
            scope.setTag("api.version", "v1");
            scope.setTag("api.endpoint", "GET /api/v1/projects/previews");
            if (appVersion != null && !appVersion.isBlank()) {
                scope.setTag("client.app_version", appVersion);
            }
        });
        log.warn("[deprecated-api] GET /api/v1/projects/previews called. userAgent={}, appVersion={}",
                userAgent, appVersion);

        List<ProjectPreviewResponse> previews = projectService.getProjectPreviewsLegacy();
        return ResponseEntity.ok(ApiResponse.success(previews));
    }

    @Operation(summary = "아티클 Project Section용 프로젝트 미리보기", description = "project_id 목록(쉼표 구분, 1개 이상)으로 아티클 내 노출용 최소 정보를 조회합니다. data.num은 반환된 프로젝트 개수이며 0이면 섹션을 표시하지 않으면 됩니다. 존재하는 프로젝트만 data.projects에 담으며 순서는 요청 ids와 동일합니다. 없는 id는 생략되며, 누락은 서버 로그(article-previews WARN)로 확인할 수 있습니다.")
    @GetMapping("/article-previews")
    public ResponseEntity<ApiResponse<ProjectArticlePreviewPayload>> getProjectArticlePreviews(
            @RequestParam(name = "ids") String ids
    ) {
        ProjectArticlePreviewPayload payload = projectService.getProjectArticlePreviews(ids);
        return ResponseEntity.ok(ApiResponse.success(payload));
    }

    @Operation(summary = "내 프로젝트 목록 조회", description = "로그인한 사용자의 프로젝트 목록을 조회합니다.")
    @GetMapping("/")
    public ResponseEntity<ApiResponse<List<ProjectDto>>> getProjects(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization") String token
    ) {
        List<ProjectDto> projects = projectService.getMyProjects(token);
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    @Operation(summary = "내 프로젝트 목록 조회", description = "로그인한 사용자의 프로젝트 목록을 조회합니다.")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ProjectDto>>> getMyProjects(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization") String token
    ) {
        List<ProjectDto> projects = projectService.getMyProjects(token);
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    @Operation(summary = "내 프로젝트 단건 조회", description = "로그인한 사용자의 특정 프로젝트의 상세 정보를 조회합니다.")
    @GetMapping("/my/{projectId}")
    public ResponseEntity<ApiResponse<ProjectDto>> getMyProject(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization") String token,
            @PathVariable Long projectId
            ){
        ProjectDto project = projectService.getMyProjectById(token, projectId);
        return ResponseEntity.ok(ApiResponse.success(project));
    }

    @Operation(summary = "프로젝트 단건 조회", description = "특정 프로젝트의 상세 정보를 조회합니다.")
    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectDto>> getProject(
            @PathVariable Long projectId
    ) {
        ProjectDto project = projectService.getProjectById(projectId);
        return ResponseEntity.ok(ApiResponse.success(project));
    }

    @Operation(summary = "기본 이미지 목록 조회",
            description = "대표사진 미지정 시 선택할 수 있는 기본 이미지 목록을 노출 순서대로 반환합니다.")
    @GetMapping("/default-thumbnails")
    public ResponseEntity<ApiResponse<List<DefaultProjectThumbnailResponse>>> getDefaultThumbnails() {
        return ResponseEntity.ok(ApiResponse.success(projectService.getDefaultThumbnails()));
    }

    @Operation(summary = "프로젝트 수정", description = "특정 프로젝트의 정보를 수정합니다.")
    @PutMapping(value = "/",  consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<ProjectDto>> updateProject(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization") String token,
            @RequestPart(value = "project") String updateProjectJson,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {

        UpdateProjectRequest request;
        try {
            request = objectMapper.readValue(updateProjectJson, UpdateProjectRequest.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON format for 'record'", e);
        }

        ProjectDto updatedProject = projectService.updateProject(token, request, file);
        return ResponseEntity.ok(ApiResponse.success(updatedProject));
    }

    @Operation(summary = "프로젝트 삭제", description = "특정 프로젝트를 삭제합니다.")
    @DeleteMapping("/{projectId}")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization") String token,
            @PathVariable Long projectId){
        projectService.deleteProject(token, projectId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

}
