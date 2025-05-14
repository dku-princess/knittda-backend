package com.example.knittdaserver.controller;

import com.example.knittdaserver.common.response.ApiResponse;
import com.example.knittdaserver.dto.CreateProjectRequest;
import com.example.knittdaserver.dto.ProjectDto;
import com.example.knittdaserver.dto.UpdateProjectRequest;
import com.example.knittdaserver.service.ProjectService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Operation(summary = "프로젝트 생성", description = "새로운 프로젝트를 생성합니다.")
    @PostMapping("/")
    public ResponseEntity<ApiResponse<ProjectDto>> createProject(
            @RequestHeader(name = "Authorization") String token,
            @RequestPart("project") @Valid CreateProjectRequest request,
            @RequestPart("file") MultipartFile file
    ) {
        ProjectDto project = projectService.createProject(token, request, file);
        return ResponseEntity.ok(ApiResponse.success(project));
    }

    @Operation(summary = "내 프로젝트 목록 조회", description = "로그인한 사용자의 프로젝트 목록을 조회합니다.")
    @GetMapping("/")
    public ResponseEntity<ApiResponse<List<ProjectDto>>> getMyProjects(
            @RequestHeader(name = "Authorization") String token
    ) {
        List<ProjectDto> projects = projectService.getMyProjects(token);
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    @Operation(summary = "프로젝트 단건 조회", description = "특정 프로젝트의 상세 정보를 조회합니다.")
    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectDto>> getProject(
            @RequestHeader(name = "Authorization") String token,
            @PathVariable Long projectId){
        ProjectDto project = projectService.getProjectById(token, projectId);
        return ResponseEntity.ok(ApiResponse.success(project));
    }

    @Operation(summary = "프로젝트 수정", description = "특정 프로젝트의 정보를 수정합니다.")
    @PutMapping("/")
    public ResponseEntity<ApiResponse<ProjectDto>> updateProject(
            @RequestHeader(name = "Authorization") String token,
            @RequestPart("project") @Valid UpdateProjectRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        ProjectDto updatedProject = projectService.updateProject(token, request, file);
        return ResponseEntity.ok(ApiResponse.success(updatedProject));
    }

    @Operation(summary = "프로젝트 삭제", description = "특정 프로젝트를 삭제합니다.")
    @DeleteMapping("/{projectId}")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @RequestHeader(name = "Authorization") String token,
            @PathVariable Long projectId){
        projectService.deleteProject(token, projectId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
