package com.example.knittdaserver.service;

import com.example.knittdaserver.common.response.ApiResponseCode;
import com.example.knittdaserver.common.response.CustomException;
import com.example.knittdaserver.dto.*;
import com.example.knittdaserver.entity.*;
import com.example.knittdaserver.repository.DesignRepository;
import com.example.knittdaserver.repository.ProjectRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@AllArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final DesignRepository designRepository;
    private final AuthService authService;
    private final RecordService recordService;
    private final S3Service s3Service;
    private final DesignService designService;

    /**
     * 프로젝트 생성
     */
    public ProjectDto createProject(String token, CreateProjectRequest request, MultipartFile file) {
        User user = authService.getUserFromJwt(token);
        Design design;
        // 필수 필드 검증
        if (request.getNickname() == null || request.getNickname().isBlank()) {
            throw new CustomException(ApiResponseCode.INVALID_INPUT);
        }


        if (request.getDesignId() != null) {
            design = designRepository.findById(request.getDesignId())
                    .orElseThrow(() -> new CustomException(ApiResponseCode.DESIGN_NOT_FOUND));

        }else {
            design = Design.builder()
                    .title(request.getTitle())
                    .designer(request.getDesigner())
                    .visible(request.isVisible())
                    .build();
            designRepository.save(design);
        }

        Project project = Project.builder()
                .user(user)
                .nickname(request.getNickname())
                .customNeedleInfo(request.getCustomNeedleInfo())
                .customYarnInfo(request.getCustomYarnInfo())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .goalDate(request.getGoalDate())
                .status(ProjectStatus.IN_PROGRESS)
                .design(design)
                .build();

        if (file != null) {
            String imageUrl = s3Service.uploadFile(file);
            Image image = Image.builder()
                    .imageUrl(imageUrl)
                    .imageOrder(1L)
                    .build();

            project.setImage(image);
        }

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
    public ProjectDto updateProject(String token, UpdateProjectRequest request, MultipartFile file) {
        User user = authService.getUserFromJwt(token);
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new CustomException(ApiResponseCode.PROJECT_NOT_FOUND));

        validateOwnership(project, user);

        updateDesign(project, request);
        updateImage(project, file);

        project.updateFromRequest(request);
        projectRepository.save(project);

        return ProjectDto.from(project);
    }

    private void updateDesign(Project project, UpdateProjectRequest request) {
        if (request.getDesignId() != null) {
            // 새로운 공식 도안으로 업데이트
            Design design = designRepository.findById(request.getDesignId())
                    .orElseThrow(() -> new CustomException(ApiResponseCode.DESIGN_NOT_FOUND));
            project.setDesign(design);
        } else {
            Design originDesign = project.getDesign();

            if (!isSameDesign(originDesign, request)) {
                // 새로운 개인 도안으로 업데이트
                CreateDesignRequest createDesignRequest = CreateDesignRequest.builder()
                        .title(request.getTitle())
                        .designer(request.getDesigner())
                        .visible(false)
                        .build();

                DesignDto designDto = designService.createDesign(createDesignRequest);
                Design design = designRepository.findById(designDto.getId())
                        .orElseThrow(() -> new CustomException(ApiResponseCode.DESIGN_NOT_FOUND));
                project.setDesign(design);
            }
        }
    }

    private boolean isSameDesign(Design originDesign, UpdateProjectRequest request) {
        if (originDesign == null) {
            return false;
        }

        String originTitle = originDesign.getTitle();
        String originDesigner = originDesign.getDesigner();
        String requestTitle = request.getTitle();
        String requestDesigner = request.getDesigner();

        return (originTitle != null && originTitle.equals(requestTitle)) &&
                (originDesigner != null && originDesigner.equals(requestDesigner));
    }

    private void updateImage(Project project, MultipartFile file) {
        if (file == null) return;

        Image existingImage = project.getImage();
        String imageUrl = s3Service.uploadFile(file);

        if (existingImage != null) {
            s3Service.deleteFile(existingImage.getImageUrl());
            existingImage.setImageUrl(imageUrl);
        } else {
            Image image = Image.builder()
                    .imageUrl(imageUrl)
                    .project(project)
                    .imageOrder(1L)
                    .build();
            project.setImage(image);
        }
    }

    /**
     * 프로젝트 삭제
     */
    public void deleteProject(String token, Long projectId) {
        User user = authService.getUserFromJwt(token);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ApiResponseCode.PROJECT_NOT_FOUND));

        validateOwnership(project, user);

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
