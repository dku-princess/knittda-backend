package com.example.knittdaserver.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.knittdaserver.common.response.ApiResponse;
import com.example.knittdaserver.dto.QuestionResponse;
import com.example.knittdaserver.entity.Project;
import com.example.knittdaserver.repository.ProjectRepository;
import com.example.knittdaserver.service.QuestionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
@Tag(name = "Question", description = "질문 관련 API")
public class QuestionController {
    private final QuestionService questionService;
    private final ProjectRepository projectRepository;
    @GetMapping("/generate/{projectId}")
    public ResponseEntity<ApiResponse<String>> generateAllQuestions(@PathVariable Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("해당 프로젝트가 존재하지 않습니다."));

        String question = questionService.generateSingleQuestion(project, 6);
        
        return ResponseEntity.ok(ApiResponse.success(question));
    }

    @Operation(summary = "프로젝트별 모든 버전 질문 조회", description = "프로젝트 ID를 기반으로 버전 1~8까지의 질문과 프로젝트 정보를 조회합니다.")
    @GetMapping("/test/project/{projectId}")
    public ResponseEntity<ApiResponse<QuestionResponse>> getAllVersionsQuestions(@PathVariable Long projectId) {
        QuestionResponse response = questionService.getAllVersionsQuestions(projectId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}