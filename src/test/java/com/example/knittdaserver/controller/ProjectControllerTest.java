//package com.example.knittdaserver.controller;
//
//import com.example.knittdaserver.common.response.ApiResponse;
//import com.example.knittdaserver.dto.CreateProjectRequest;
//import com.example.knittdaserver.dto.ProjectDto;
//import com.example.knittdaserver.dto.UpdateProjectRequest;
//import com.example.knittdaserver.entity.*;
//import com.example.knittdaserver.repository.DesignRepository;
//import com.example.knittdaserver.repository.ProjectRepository;
//import com.example.knittdaserver.repository.UserRepository;
//import com.example.knittdaserver.util.JwtUtil;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.ResponseEntity;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.util.List;
//import java.util.Objects;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//@SpringBootTest
//@Transactional
//class ProjectControllerTest {
//
//
//    @Autowired
//    private JwtUtil jwtUtil;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private ProjectRepository projectRepository;
//
//    @Autowired
//    private ProjectController projectController;
//
//    @Autowired
//    private DesignRepository designRepository;
//
//    private User user;
//    private String token;
//    private Design design;
//
//    @BeforeEach
//    void setup() {
//
//        // 유저 생성
//        user = User.builder()
//                .kakaoId(1L)
//                .email("test@example.com")
//                .nickname("testUser")
//                .build();
//        userRepository.save(user);
//
//        // JWT 토큰 생성
//        token = jwtUtil.generateToken(user.getId());
//
//        // 도안 저장
//        design = Design.builder()
//                .title("test_design")
//                .build();
//        designRepository.save(design);
//
//    }
////    @Test
////    void createProject_Success() {
////        Design testDesign = designRepository.findByTitle("test_design");
////        User testUser = userRepository.findByKakaoId(1L)
////                .orElseThrow(() -> new RuntimeException("no such User"));
////        // Given - 요청 생성
////        CreateProjectRequest request = CreateProjectRequest.builder()
////                .designId(testDesign.getId())
////                .nickname("Test Project")
////                .build();
////
////
////        MultipartFile multipartFile = null;
////
////        // When - 프로젝트 생성 요청
////        projectController.createProject(token, request,image);
////
////        // Then - 프로젝트가 저장되었는지 검증
////        List<Project> projects = projectRepository.findByUserId(testUser.getId());
////
////        System.out.println("projects = " + projects.toString());
////        System.out.println("project = " + projects.get(0).toString());
////        System.out.println("testUser = " + testUser.toString());
////
////        assertNotNull(projects);
////        assertEquals(1, projects.size());
////        assertEquals("Test Project", projects.get(0).getNickname());
////        assertEquals(projects.get(0).getUser(), testUser);
////    }
//
//    @Test
//    void getMyProjects() {
//        // Given
//        Design testDesign = designRepository.findByTitle("test_design");
//        User testUser = userRepository.findByKakaoId(1L)
//                .orElseThrow(() -> new RuntimeException("no such User"));
//
//        Project testProject = Project.builder()
//                .design(testDesign)
//                .nickname("test Project")
//                .user(testUser)
//                .build();
//
//        projectRepository.save(testProject);
//
//        // When
//        ResponseEntity<ApiResponse<List<ProjectDto>>> myProjects = projectController.getMyProjects(token);
//        List<ProjectDto> projectDtos = Objects.requireNonNull(myProjects.getBody()).getData();
//
//        System.out.println("projectDtos = " + projectDtos.toString());
//        System.out.println("projectDtos = " + projectDtos.get(0).toString());
//        // Then
//        assertNotNull(projectDtos);
//        assertEquals(1, projectDtos.size());
//        assertEquals(projectDtos.get(0).getNickname(), testProject.getNickname());
//    }
//
//    @Test
//    void getProject() {
//        // Given
//        Design testDesign = designRepository.findByTitle("test_design");
//        User testUser = userRepository.findByKakaoId(1L)
//                .orElseThrow(() -> new RuntimeException("no such User"));
//
//        Project testProject1 = Project.builder()
//                .design(testDesign)
//                .nickname("test Project1")
//                .user(testUser)
//                .build();
//
//        Project testProject2 = Project.builder()
//                .design(testDesign)
//                .nickname("test Project2")
//                .user(testUser)
//                .build();
//
//        projectRepository.save(testProject1);
//        projectRepository.save(testProject2);
//
//        // When
//        ProjectDto response1 = projectController.getProject(token, testProject1.getId()).getBody().getData();
//        ProjectDto response2 = projectController.getProject(token, testProject2.getId()).getBody().getData();
//
//        // Then
//        assertNotNull(response1);
//        assertNotNull(response2);
//        System.out.println("response1 = " + response1);
//        System.out.println("response2 = " + response2);
//        assertEquals(response1.getNickname(), testProject1.getNickname());
//        assertEquals(response2.getNickname(), testProject2.getNickname());
//
//    }
//
//    @Test
//    void updateProject() {
//        Project project = Project.builder()
//                .design(design)
//                .nickname("test Project")
//                .customYarnInfo("custom YarnInfo")
//                .customNeedleInfo("custom NeedleInfo")
//                .user(user)
//                .build();
//
//        projectRepository.save(project);
//
//        UpdateProjectRequest request = UpdateProjectRequest.builder()
//                .projectId(project.getId())
//                .nickname("update test Project")
//                .customYarnInfo("update custom YarnInfo")
//                .status(ProjectStatus.DONE)
//                .build();
//
//        // Given
//        List<ProjectDto> projectDtos = projectController.getMyProjects(token).getBody().getData();
//        ProjectDto data = projectController.updateProject(token, request).getBody().getData();
//        List<ProjectDto> updateProjectDtos = projectController.getMyProjects(token).getBody().getData();
//
//        //Then
//        assertNotNull(data);
//        assertEquals("update test Project", data.getNickname());
//        assertEquals("update custom YarnInfo", data.getCustomYarnInfo());
//        assertEquals("custom NeedleInfo", data.getCustomNeedleInfo());
//
//        assertEquals(1, projectDtos.size());
//        assertEquals("update test Project", updateProjectDtos.get(0).getNickname());
//        System.out.println("projectDtos = " + projectDtos.toString());
//        System.out.println("updateProjectDtos = " + updateProjectDtos.toString());
//
//    }
//
//    @Test
//    void deleteProject() {
//        //Then
//        Project project = Project.builder()
//                .design(design)
//                .nickname("test Project")
//                .customYarnInfo("custom YarnInfo")
//                .customNeedleInfo("custom NeedleInfo")
//                .user(user)
//                .build();
//
//        projectRepository.save(project);
//
//        //When
//        ApiResponse<Void> apiResponse = projectController.deleteProject(token, project.getId()).getBody();
//        Optional<Project> projectOptional = projectRepository.findById(project.getId());
//
//        //Then
//        assertNotNull(apiResponse);
//        assertTrue(projectOptional.isEmpty());
//    }
//}