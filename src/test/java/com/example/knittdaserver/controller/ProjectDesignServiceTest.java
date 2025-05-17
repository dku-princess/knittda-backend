package com.example.knittdaserver.controller;

import com.example.knittdaserver.entity.Design;
import com.example.knittdaserver.entity.Project;
import com.example.knittdaserver.entity.User;
import com.example.knittdaserver.repository.DesignRepository;
import com.example.knittdaserver.repository.UserRepository;
import com.example.knittdaserver.service.DesignService;
import com.example.knittdaserver.service.ProjectService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Transactional
@SpringBootTest
public class ProjectDesignServiceTest {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private DesignService designService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DesignRepository designRepository;

    @Autowired
    private ProjectController projectController;

    private Design testDesign;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .nickname("testUser")
                .build();

        userRepository.save(testUser);

        testDesign = Design.builder()
                .title("official Title")
                .designer("official Designer")
                .build();

        designRepository.save(testDesign);
    }
    // 공식 도안을 사용한 프로젝트 생성
//    @Test
//    void createProjectWithOfficialDesign() {
//        //Given
//        Project.builder()
//                .user(testUser)
//                .design(testDesign)
//        //When
//        projectController.createProject()
//
//
//    }
}
