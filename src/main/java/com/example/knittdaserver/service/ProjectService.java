package com.example.knittdaserver.service;

import com.example.knittdaserver.common.response.ApiResponseCode;
import com.example.knittdaserver.common.response.CustomException;
import com.example.knittdaserver.dto.CreateProjectRequest;
import com.example.knittdaserver.dto.ProjectDto;
import com.example.knittdaserver.dto.UpdateProjectRequest;
import com.example.knittdaserver.entity.Design;
import com.example.knittdaserver.entity.Project;
import com.example.knittdaserver.entity.ProjectStatus;
import com.example.knittdaserver.entity.User;
import com.example.knittdaserver.repository.DesignRepository;
import com.example.knittdaserver.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final DesignRepository designRepository;
    private final AuthService authService;
    private final RecordService recordService;
    /**
     * 프로젝트 생성
     */
    public ProjectDto createProject(String token, CreateProjectRequest request) {

        User user = authService.getUserFromJwt(token);


        Design design = designRepository.findById(request.getDesignId())
                .orElseThrow(() -> new CustomException(ApiResponseCode.DESIGN_NOT_FOUND));

        Project project = Project.builder()
                .design(design)
                .user(user)
                .nickname(request.getNickname())
                .customNeedleInfo(request.getCustomNeedleInfo())
                .customYarnInfo(request.getCustomYarnInfo())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .goalDate(request.getGoalDate())
                .status(ProjectStatus.IN_PROGRESS)
                .build();

        return ProjectDto.from(projectRepository.save(project));
    }

    /**
     * 내 프로젝트 목록 조회
     */
    public List<ProjectDto> getMyProjects(String token) {
        User user = authService.getUserFromJwt(token);

        List<Project> projects = projectRepository.findByUserId(user.getId());
        return projects.stream()
                .map(ProjectDto::from)
                .toList();
    }

    /**
     * 프로젝트 단건 조회
     */
    public ProjectDto getProjectById(String token, Long projectId) {
        User user = authService.getUserFromJwt(token);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ApiResponseCode.PROJECT_NOT_FOUND));

        validateOwnership(project, user);

        return ProjectDto.from(project);
    }

    /**
     * 프로젝트 수정
     */
    public ProjectDto updateProject(String token, UpdateProjectRequest request) {
        User user = authService.getUserFromJwt(token);
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new CustomException(ApiResponseCode.PROJECT_NOT_FOUND));

        validateOwnership(project, user);

        project.updateFromRequest(request);
        projectRepository.save(project);

        return ProjectDto.from(project);
    }

    /**
     * 프로젝트 삭제
     */
    public void deleteProject(String token, Long projectId) {
        User user = authService.getUserFromJwt(token);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ApiResponseCode.PROJECT_NOT_FOUND));

        validateOwnership(project, user);

        recordService.deleteRecordsByProjectId(token, projectId);
        projectRepository.delete(project);
    }

    /**
     * 사용자 소유 여부 확인
     */
    private void validateOwnership(Project project, User user) {
        if (!project.isOwnedBy(user.getId())) {
            throw new CustomException(ApiResponseCode.FORBIDDEN_ACCESS);
        }
    }
}
