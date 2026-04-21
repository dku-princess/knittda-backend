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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import java.util.List;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@AllArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final DesignRepository designRepository;
    private final ImageRepository imageRepository;
    private final ThumbnailImageRepository thumbnailImageRepository;
    private final AuthService authService;
    private final S3Service s3Service;
    private final FileUploadService fileUploadService;

    /**
     * 프로젝트 생성
     */
    @Transactional
    public ProjectDto createProject(String token, CreateProjectRequest request, MultipartFile file) {
        User user = authService.getUserFromJwt(token);
        // 필수 필드 검증
        if (request.getNickname() == null || request.getNickname().isBlank()) {
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
            log.info("[ProjectService] 프로젝트 생성 - 썸네일 이미지 업로드 시작 - 프로젝트 ID: {}, 파일명: {}, 크기: {} bytes", 
                    project.getId(), file.getOriginalFilename(), file.getSize());
            try {
                String imageUrl = fileUploadService.uploadImageAsWebp(file);
                log.info("[ProjectService] 프로젝트 생성 - 썸네일 이미지 업로드 완료 - 프로젝트 ID: {}, URL: {}", 
                        project.getId(), imageUrl);
                
                ThumbnailImage thumbnailImage = ThumbnailImage.builder()
                        .imageUrl(imageUrl)
                        .build();
                // ThumbnailImage를 먼저 저장
                thumbnailImage = thumbnailImageRepository.save(thumbnailImage);
                project.setThumbnail(thumbnailImage);
                log.info("[ProjectService] 프로젝트 생성 - 썸네일 이미지 저장 완료 - 프로젝트 ID: {}, 썸네일 ID: {}", 
                        project.getId(), thumbnailImage.getId());
            } catch (IOException e) {
                log.error("[ProjectService] 프로젝트 생성 - 썸네일 이미지 업로드 실패 - 프로젝트 ID: {}, 파일명: {}, 크기: {} bytes, 에러: {}", 
                        project.getId(), file.getOriginalFilename(), file.getSize(), e.getMessage(), e);
                // 이미지 업로드 실패해도 프로젝트는 생성되도록 함 (선택적 필드)
                // 필요시 예외를 다시 던져서 프로젝트 생성 자체를 실패시킬 수 있음
            } catch (Exception e) {
                log.error("[ProjectService] 프로젝트 생성 - 썸네일 이미지 업로드 중 예상치 못한 에러 - 프로젝트 ID: {}, 파일명: {}, 에러: {}", 
                        project.getId(), file.getOriginalFilename(), e.getMessage(), e);
            }
        } else {
            log.info("[ProjectService] 프로젝트 생성 - 썸네일 이미지 없음 - 프로젝트 ID: {}", project.getId());
        }

        return ProjectDto.from(projectRepository.save(project));
    }

    /**
     * 내 프로젝트 목록 조회
     */
    @Transactional
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
    @Transactional
    public ProjectDto getProjectById(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(ApiResponseCode.PROJECT_NOT_FOUND));

        return ProjectDto.from(project);
    }


        /**
     * 프로젝트 단건 조회
     */
    @Transactional
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
        if (file == null) {
            log.debug("[ProjectService] 프로젝트 수정 - 이미지 파일 없음, 업데이트 건너뜀 - 프로젝트 ID: {}", 
                    project.getId());
            return;
        }

        log.info("[ProjectService] 프로젝트 수정 - 썸네일 이미지 업데이트 시작 - 프로젝트 ID: {}, 파일명: {}, 크기: {} bytes", 
                project.getId(), file.getOriginalFilename(), file.getSize());

        try {
            String imageUrl = fileUploadService.uploadImageAsWebp(file);
            log.info("[ProjectService] 프로젝트 수정 - 새 썸네일 이미지 업로드 완료 - 프로젝트 ID: {}, URL: {}", 
                    project.getId(), imageUrl);
            
            ThumbnailImage thumbnailImage = project.getThumbnail();

            if (thumbnailImage == null) {
                log.info("[ProjectService] 프로젝트 수정 - 새 썸네일 이미지 생성 - 프로젝트 ID: {}", project.getId());
                thumbnailImage = ThumbnailImage.builder()
                        .imageUrl(imageUrl)
                        .build();
                // 새로운 ThumbnailImage를 저장
                thumbnailImage = thumbnailImageRepository.save(thumbnailImage);
                log.info("[ProjectService] 프로젝트 수정 - 새 썸네일 이미지 저장 완료 - 프로젝트 ID: {}, 썸네일 ID: {}", 
                        project.getId(), thumbnailImage.getId());
            } else {
                String prevThumbnailUrl = thumbnailImage.getImageUrl();
                log.info("[ProjectService] 프로젝트 수정 - 기존 썸네일 이미지 업데이트 - 프로젝트 ID: {}, 기존 URL: {}, 새 URL: {}", 
                        project.getId(), prevThumbnailUrl, imageUrl);
                
                thumbnailImage.setImageUrl(imageUrl);
                // 기존 ThumbnailImage 업데이트
                thumbnailImage = thumbnailImageRepository.save(thumbnailImage);

                // S3 삭제는 DB 저장 후, 실패 시 예외 처리
                if (prevThumbnailUrl != null && !prevThumbnailUrl.isEmpty()) {
                    try {
                        log.info("[ProjectService] 프로젝트 수정 - 기존 썸네일 이미지 S3 삭제 시작 - 프로젝트 ID: {}, URL: {}", 
                                project.getId(), prevThumbnailUrl);
                        s3Service.deleteFile(prevThumbnailUrl);
                        log.info("[ProjectService] 프로젝트 수정 - 기존 썸네일 이미지 S3 삭제 완료 - 프로젝트 ID: {}", 
                                project.getId());
                    } catch (Exception e) {
                        log.warn("[ProjectService] 프로젝트 수정 - 기존 썸네일 이미지 S3 삭제 실패 (무시) - 프로젝트 ID: {}, URL: {}, 에러: {}", 
                                project.getId(), prevThumbnailUrl, e.getMessage(), e);
                        // S3 삭제 실패해도 프로세스는 계속 진행
                    }
                }
            }
            project.setThumbnail(thumbnailImage);
            log.info("[ProjectService] 프로젝트 수정 - 썸네일 이미지 업데이트 완료 - 프로젝트 ID: {}", project.getId());

        } catch (IOException e) {
            log.error("[ProjectService] 프로젝트 수정 - 썸네일 이미지 업로드 실패 - 프로젝트 ID: {}, 파일명: {}, 크기: {} bytes, 에러: {}", 
                    project.getId(), file.getOriginalFilename(), file.getSize(), e.getMessage(), e);
            throw new CustomException(ApiResponseCode.IMAGE_UPLOAD_FAILED);
        } catch (Exception e) {
            log.error("[ProjectService] 프로젝트 수정 - 썸네일 이미지 업데이트 중 예상치 못한 에러 - 프로젝트 ID: {}, 파일명: {}, 에러: {}", 
                    project.getId(), file.getOriginalFilename(), e.getMessage(), e);
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
    @Transactional
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

    /**
     * 아티클 Project Section용 프로젝트 미리보기 (요청 순서 유지, 중복 ID는 첫 등장 순서 기준으로 한 번만 반환)
     */
    @Transactional
    public ProjectArticlePreviewPayload getProjectArticlePreviews(String idsParam) {
        if (idsParam == null || idsParam.isBlank()) {
            throw new CustomException(ApiResponseCode.INVALID_INPUT);
        }

        List<Long> orderedUniqueIds = parseAndDedupeOrderedIds(idsParam);

        List<Project> found = projectRepository.findAllByIdIn(orderedUniqueIds);
        Map<Long, Project> byId = new LinkedHashMap<>();
        for (Project project : found) {
            byId.put(project.getId(), project);
        }

        List<ProjectArticlePreviewResponse> result = new ArrayList<>();
        List<Long> missingProjectIds = new ArrayList<>();
        for (Long id : orderedUniqueIds) {
            Project project = byId.get(id);
            if (project == null) {
                missingProjectIds.add(id);
                continue;
            }
            String thumbnailUrl = project.getThumbnail() != null ? project.getThumbnail().getImageUrl() : null;
            String ownerNickname = project.getUser() != null ? project.getUser().getNickname() : null;
            result.add(new ProjectArticlePreviewResponse(
                    project.getId(),
                    project.getNickname(),
                    thumbnailUrl,
                    ownerNickname
            ));
        }

        if (!missingProjectIds.isEmpty()) {
            log.warn("[article-previews] 요청 ids 중 DB에 없는 project_id가 있습니다. missingProjectIds={}", missingProjectIds);
        }

        return new ProjectArticlePreviewPayload(result.size(), result);
    }

    private static List<Long> parseAndDedupeOrderedIds(String idsParam) {
        String[] tokens = idsParam.split(",");
        List<Long> orderedUnique = new ArrayList<>();
        HashSet<Long> seen = new HashSet<>();

        for (String token : tokens) {
            if (token == null) {
                continue;
            }
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                throw new CustomException(ApiResponseCode.INVALID_INPUT);
            }
            long id;
            try {
                id = Long.parseLong(trimmed);
            } catch (NumberFormatException e) {
                throw new CustomException(ApiResponseCode.INVALID_INPUT);
            }
            if (seen.add(id)) {
                orderedUnique.add(id);
            }
        }

        if (orderedUnique.isEmpty()) {
            throw new CustomException(ApiResponseCode.INVALID_INPUT);
        }
        return orderedUnique;
    }    
}
