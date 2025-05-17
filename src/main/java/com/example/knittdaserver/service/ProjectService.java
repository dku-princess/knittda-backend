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

    private final RecordService recordService;
    /**
     * 프로젝트 생성
     */
    public ProjectDto createProject(String token, CreateProjectRequest request, MultipartFile file) {
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


        if (request.getDesignId() != null) {
            Design design = designRepository.findById(request.getDesignId())
                    .orElseThrow(() -> new CustomException(ApiResponseCode.DESIGN_NOT_FOUND));

            project.setDesign(design);

        }
        else if (request.getDesignInfo() != null) {
            CreateDesignRequest designRequest = CreateDesignRequest.builder()
                    .title(request.getDesignInfo().getTitle())
                    .designer(request.getDesignInfo().getDesigner())
                    .build();
            DesignDto designDto = designService.createDesign(designRequest);
            Design design = designRepository.findById(designDto.getId())
                    .orElseThrow(() -> new CustomException(ApiResponseCode.DESIGN_NOT_FOUND));

            project.setDesign(design);
        } else {
            throw new CustomException(ApiResponseCode.INVALID_DESIGN_INFO);
        }

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

        if (file != null) {
            //기존 이미지 파일
            Image existingImage = project.getImage();

            if (existingImage != null) {
                s3Service.deleteFile(existingImage.getImageUrl());

                // 새로운 이미지 파일 업로드
                String imageUrl = s3Service.uploadFile(file);
                existingImage.setImageUrl(imageUrl);
            } else {
                String imageUrl = s3Service.uploadFile(file);

                Image image = Image.builder()
                        .imageUrl(imageUrl)
                        .project(project)
                        .imageOrder(1L)
                        .build();
                project.setImage(image);
            }

        }

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
