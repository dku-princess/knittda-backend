package com.example.knittdaserver.service;

import com.example.knittdaserver.common.response.ApiResponseCode;
import com.example.knittdaserver.common.response.CustomException;
import com.example.knittdaserver.dto.*;
import com.example.knittdaserver.entity.*;
import com.example.knittdaserver.entity.Record;
import com.example.knittdaserver.repository.DesignRepository;
import com.example.knittdaserver.repository.ImageRepository;
import com.example.knittdaserver.repository.ProjectRepository;
import com.example.knittdaserver.repository.ThumbnailImageRepository;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

@Service
@AllArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final DesignRepository designRepository;
    private final ImageRepository imageRepository;
    private final ThumbnailImageRepository thumbnailImageRepository;
    private final AuthService authService;
    private final S3Service s3Service;
    private final DesignService designService;
    private final FileUploadService fileUploadService;

    /**
     * 프로젝트 생성
     */
    @Transactional
    public ProjectDto createProject(String token, CreateProjectRequest request, MultipartFile file) {
        User user = authService.getUserFromJwt(token);
        // 필수 필드 검증
        if (request.getNickname() == null || request.getNickname().isBlank() || file == null) {
            throw new CustomException(ApiResponseCode.INVALID_INPUT);
        }

        Design design = Design.builder()
                .title(request.getDesignTitle())
                .designer(request.getDesigner())
                .needleInfo(request.getNeedleInfo())
                .yarnInfo(request.getYarnInfo())
                .description(request.getDescription())
                .build();

        designRepository.save(design);
        

        Project project = Project.builder()
                .user(user)
                .nickname(request.getNickname())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .goalDate(request.getGoalDate())
                .status(ProjectStatus.IN_PROGRESS)
                .design(design)
                .build();

        projectRepository.save(project);

        // 썸네일 이미지 업로드
        if (file != null) {
            try {
                String imageUrl = fileUploadService.uploadImageAsWebp(file);
                ThumbnailImage thumbnailImage = ThumbnailImage.builder()
                        .imageUrl(imageUrl)
                        .build();
                // ThumbnailImage를 먼저 저장
                thumbnailImage = thumbnailImageRepository.save(thumbnailImage);
                project.setThumbnail(thumbnailImage);
            } catch (IOException e) {
                e.printStackTrace();
            }
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
    public ProjectDto getProjectById(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ApiResponseCode.PROJECT_NOT_FOUND));

        return ProjectDto.from(project);
    }


        /**
     * 프로젝트 단건 조회
     */
    public ProjectDto getMyProjectById(String token, Long projectId) {
        User user = authService.getUserFromJwt(token);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ApiResponseCode.PROJECT_NOT_FOUND));

        validateOwnership(project, user);

        return ProjectDto.from(project);
    }

    /**
     * 프로젝트 수정
     */
    @Transactional
    public ProjectDto updateProject(String token, UpdateProjectRequest request, MultipartFile file) {
        User user = authService.getUserFromJwt(token);
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new CustomException(ApiResponseCode.PROJECT_NOT_FOUND));

        // 소유자 확인
        validateOwnership(project, user);

        // 도안 업데이트
        updateDesign(project, request);

        // 썸네일 이미지 업데이트
        updateImage(project, file);

        // 프로젝트 정보 업데이트
        project.updateFromRequest(request);
        projectRepository.save(project);

        return ProjectDto.from(project);
    }
 
    @Transactional
    private void updateDesign(Project project, UpdateProjectRequest request) {

        Design originDesign = project.getDesign();

        if (originDesign == null) {
            Design design = Design.builder()
                .title(request.getDesignTitle())
                .designer(request.getDesigner())
                .needleInfo(request.getNeedleInfo())
                .yarnInfo(request.getYarnInfo())
                .description(request.getDescription())
                .build();
            designRepository.save(design);
            project.setDesign(design);
        }
        else {
            originDesign.updateFromRequest(request);
            designRepository.save(originDesign);
        }
    }


    @Transactional
    private void updateImage(Project project, MultipartFile file) {
        if (file == null) return;

        try {
            String imageUrl = fileUploadService.uploadImageAsWebp(file);
            ThumbnailImage thumbnailImage = project.getThumbnail();

            if (thumbnailImage == null) {
                thumbnailImage = ThumbnailImage.builder()
                        .imageUrl(imageUrl)
                        .build();
                // 새로운 ThumbnailImage를 저장
                thumbnailImage = thumbnailImageRepository.save(thumbnailImage);
            } else {
                String prevThumbnailUrl = thumbnailImage.getImageUrl();
                thumbnailImage.setImageUrl(imageUrl);
                // 기존 ThumbnailImage 업데이트
                thumbnailImage = thumbnailImageRepository.save(thumbnailImage);

                // S3 삭제는 DB 저장 후, 실패 시 예외 처리
                if (prevThumbnailUrl != null && !prevThumbnailUrl.isEmpty()) {
                    try {
                        s3Service.deleteFile(prevThumbnailUrl);
                    } catch (Exception e) {
                        // 로그 남기기
                        // log.warn("S3 이미지 삭제 실패: {}", prevThumbnailUrl, e); // log 객체가 없어서 주석 처리
                    }
                }
            }
            project.setThumbnail(thumbnailImage);

        } catch (IOException e) {
            throw new CustomException(ApiResponseCode.IMAGE_UPLOAD_FAILED);
        }
    }

    /**
     * 프로젝트 삭제
     */
    @Transactional
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
    public void validateOwnership(Project project, User user) {
        if (!project.isOwnedBy(user.getId())) {
            throw new CustomException(ApiResponseCode.FORBIDDEN_ACCESS);
        }
    }

    // 전체 프로젝트 미리보기 조회
    public List<ProjectPreviewResponse> getProjectPreviews() {
        List<Project> projects = projectRepository.findAllByOrderByLastRecordAtDesc();
        List<ProjectPreviewResponse> responses = new ArrayList<>();

        for (Project project : projects) {
            List<Long> recordIds = project.getRecords().stream()
                .map(Record::getId)
                .toList();

            List<Image> recentImages = recordIds.isEmpty()
                ? new ArrayList<>()
                : new ArrayList<>(imageRepository.findTop3ByRecordIdInOrderByCreatedAtDesc(recordIds));

            if (recentImages.size() < 3 && project.getThumbnail() != null) {
                // ThumbnailImage를 Image로 변환하여 추가
                Image thumbnailAsImage = Image.builder()
                        .imageUrl(project.getThumbnail().getImageUrl())
                        .build();
                recentImages.add(thumbnailAsImage);
            }

            List<String> imageUrls = recentImages.stream()
                .filter(Objects::nonNull) // null 값 필터링
                .map(Image::getImageUrl)
                .toList();

            responses.add(ProjectPreviewResponse.builder()
                .projectId(project.getId())
                .userName(project.getUser().getNickname())
                .projectName(project.getNickname())
                .recordNum(project.getRecords().size())
                .recentImageUrls(imageUrls)
                .lastRecordAt(project.getLastRecordAt())
                .build());
        }

        return responses;
    }
}
