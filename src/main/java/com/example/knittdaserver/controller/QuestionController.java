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
import com.example.knittdaserver.entity.Project;
import com.example.knittdaserver.repository.ProjectRepository;
import com.example.knittdaserver.service.QuestionService;

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

    @GetMapping("/generate/test/{projectId}")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> generateAllQuestionsTest(@PathVariable Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("해당 프로젝트가 존재하지 않습니다."));
        
        List<Map<String, String>> questions = new ArrayList<>();
        
        // v1부터 v5까지 모든 버전의 질문 생성
        for (int version = 1; version <= 6; version++) {
            Map<String, String> questionMap = new HashMap<>();
            questionMap.put("version", String.valueOf(version));
            questionMap.put("question", questionService.generateSingleQuestion(project, version));
            questions.add(questionMap);
        }
        
        return ResponseEntity.ok(ApiResponse.success(questions));
    }

    @GetMapping("/generate/{projectId}")
    public ResponseEntity<ApiResponse<String>> generateAllQuestions(@PathVariable Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("해당 프로젝트가 존재하지 않습니다."));

        String question = questionService.generateSingleQuestion(project, 4);
        
        return ResponseEntity.ok(ApiResponse.success(question));
    }

}