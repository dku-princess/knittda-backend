package com.example.knittdaserver.controller;

import com.example.knittdaserver.common.response.ApiResponse;
import com.example.knittdaserver.dto.ProjectPreviewResponse;
import com.example.knittdaserver.service.ProjectService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.sentry.Sentry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Project V2", description = "프로젝트 관련 API (v2 — 페이지네이션 도입)")
@Slf4j
@RestController
@RequestMapping("/api/v2/projects")
@RequiredArgsConstructor
public class ProjectV2Controller {

    private final ProjectService projectService;
    private final MeterRegistry meterRegistry;

    // v2 endpoint 호출 카운터 — v1 counter 와 동일한 메트릭 이름(api.previews.calls)
    // 으로 두면 PromQL 에서 version 라벨로 group by 하여 비율 비교 가능.
    private Counter v2PreviewsCounter;

    @PostConstruct
    void initMetrics() {
        this.v2PreviewsCounter = Counter.builder("api.previews.calls")
                .description("project previews endpoint invocations")
                .tag("endpoint", "GET /api/v2/projects/previews")
                .tag("version", "v2")
                .register(meterRegistry);
    }

    @Operation(summary = "프로젝트 미리보기 조회 (페이지네이션)",
            description = "프로젝트 미리보기를 페이지네이션하여 조회합니다. page(기본 0), size(기본 50, 최대 50). " +
                    "응답은 Spring Data Page 형식 (data.content, data.last, data.number 등).")
    @GetMapping("/previews")
    public ResponseEntity<ApiResponse<Page<ProjectPreviewResponse>>> getProjectPreviews(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size,
            @RequestHeader(name = "X-App-Version", required = false) String appVersion
    ) {
        // [VERSION TRACKING]
        // 1) Micrometer Counter — Prometheus 에서 version 라벨로 v1/v2 비율 산출
        v2PreviewsCounter.increment();
        // 2) Sentry 태깅 — 트레이스에 컨텍스트 부여
        Sentry.configureScope(scope -> {
            scope.setTag("api.version", "v2");
            scope.setTag("api.endpoint", "GET /api/v2/projects/previews");
            if (appVersion != null && !appVersion.isBlank()) {
                scope.setTag("client.app_version", appVersion);
            }
        });

        Page<ProjectPreviewResponse> previews = projectService.getProjectPreviews(page, size);
        return ResponseEntity.ok(ApiResponse.success(previews));
    }
}
