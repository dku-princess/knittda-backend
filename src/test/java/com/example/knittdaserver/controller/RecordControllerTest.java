//package com.example.knittdaserver.controller;
//
//import com.example.knittdaserver.common.response.ApiResponseCode;
//import com.example.knittdaserver.common.response.CustomException;
//import com.example.knittdaserver.dto.CreateRecordRequest;
//import com.example.knittdaserver.dto.RecordResponse;
//import com.example.knittdaserver.dto.UpdateRecordRequest;
//import com.example.knittdaserver.entity.Design;
//import com.example.knittdaserver.entity.Project;
//import com.example.knittdaserver.entity.Record;
//import com.example.knittdaserver.entity.RecordStatus;
//import com.example.knittdaserver.entity.User;
//import com.example.knittdaserver.repository.DesignRepository;
//import com.example.knittdaserver.repository.ProjectRepository;
//import com.example.knittdaserver.repository.RecordRepository;
//import com.example.knittdaserver.repository.UserRepository;
//import com.example.knittdaserver.service.AuthService;
//import com.example.knittdaserver.service.ProjectService;
//import com.example.knittdaserver.service.RecordService;
//import com.example.knittdaserver.util.JwtUtil;
//import jakarta.transaction.Transactional;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@Transactional
//@SpringBootTest
//class RecordControllerTest {
//
//    @Autowired
//    private RecordService recordService;
//
//    @Autowired
//    private RecordController recordController;
//
//    @Autowired
//    private AuthService authService;
//
//    @Autowired
//    private DesignRepository designRepository;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private ProjectRepository projectRepository;
//
//    @Autowired
//    private RecordRepository recordRepository;
//
//    private Project testProject;
//    private User testUser;
//    private String token;
//
//    private Design testDesign;
//    @Autowired
//    private JwtUtil jwtUtil;
//    @Autowired
//    private ProjectService projectService;
//
//    @BeforeEach
//    void setUp() {
//        // Design
//        testDesign = Design.builder()
//                .title("test design")
//                .build();
//
//        designRepository.save(testDesign);
//        // User
//        testUser = User.builder()
//                .kakaoId(1L)
//                .nickname("test user")
//                .build();
//
//        userRepository.save(testUser);
//        token = jwtUtil.generateToken(testUser.getId());
//
//        // Project
//        testProject = Project.builder()
//                .user(testUser)
//                .design(testDesign)
//                .nickname("test project")
//                .build();
//        projectRepository.save(testProject);
//    }
//
//    @Test
//    void createRecord() {
//        //Given
//        CreateRecordRequest request = CreateRecordRequest.builder()
//                .projectId(testProject.getId())
//                .comment("test comment")
//                .recordStatus(RecordStatus.STARTED)
//                .tags(List.of("tag1", "tag2", "tag3"))
//                .build();
//
//        //When
//        recordController.createRecord(token, request);
//        List<Record> records = recordRepository.findByProjectId(testProject.getId());
//
//        //Then
//        assertNotNull(records);
//        assertEquals(1, records.size());
//        Record record = records.get(0);
//        assertNotNull(record);
//        assertEquals("test comment", record.getComment());
//        System.out.println("record = " + record.toString());
//    }
//
//    @Test
//    void getAllRecords() {
//        //Given
//        Record record1 = Record.builder()
//                .project(testProject)
//                .recordStatus(RecordStatus.STARTED)
//                .tags(List.of("tag1", "tag2", "tag3"))
//                .comment("test comment")
//                .build();
//
//        Record record2 = Record.builder()
//                .project(testProject)
//                .recordStatus(RecordStatus.STARTED)
//                .comment("test comment 2")
//                .build();
//
//        recordRepository.save(record1);
//        recordRepository.save(record2);
//
//        List<RecordResponse> data = recordController.getAllRecords(token).getBody().getData();
//        assertNotNull(data);
//        assertEquals(2, data.size());
//        assertEquals("test comment", data.get(0).getComment());
//        assertEquals("test comment 2", data.get(1).getComment());
//    }
//
//    @Test
//    void getRecordsByProjectId() {
//        Design testDesign2 = Design.builder()
//                .title("test design 2")
//                .build();
//
//        designRepository.save(testDesign2);
//
//
//        // Given
//        Project testProject2 = Project.builder()
//                .user(testUser)
//                .design(testDesign2)
//                .nickname("test project 2")
//                .build();
//
//        projectRepository.save(testProject2);
//
//        Record record1 = Record.builder()
//                .project(testProject)
//                .recordStatus(RecordStatus.STARTED)
//                .tags(List.of("tag1", "tag2", "tag3"))
//                .comment("test comment")
//                .build();
//
//        Record record2 = Record.builder()
//                .project(testProject)
//                .recordStatus(RecordStatus.STARTED)
//                .comment("test comment 2")
//                .build();
//
//        Record record3 = Record.builder()
//                .project(testProject2)
//                .recordStatus(RecordStatus.STARTED)
//                .comment("test comment for testProject2")
//                .build();
//
//        recordRepository.save(record1);
//        recordRepository.save(record2);
//        recordRepository.save(record3);
//
//        // When
//        List<RecordResponse> recordForProject1 = recordController.getRecordsByProjectId(token, testProject.getId()).getBody().getData();
//        List<RecordResponse> recordForProject2 = recordController.getRecordsByProjectId(token, testProject2.getId()).getBody().getData();
//
//        // Then
//        assertNotNull(recordForProject1);
//        assertNotNull(recordForProject2);
//        assertEquals(2, recordForProject1.size());
//        assertEquals(1, recordForProject2.size());
//
//        assertEquals("test comment", recordForProject1.get(0).getComment());
//        assertEquals("test comment 2", recordForProject1.get(1).getComment());
//        assertEquals("test comment for testProject2", recordForProject2.get(0).getComment());
//
//        System.out.println("recordForProject1 = " + recordForProject1.toString());
//        System.out.println("recordForProject2 = " + recordForProject2.get(0).toString());
//
//    }
//
//    @Test
//    void getRecordById() {
//        // Given
//        Record record1 = Record.builder()
//                .project(testProject)
//                .recordStatus(RecordStatus.STARTED)
//                .tags(List.of("tag1", "tag2", "tag3"))
//                .comment("test comment")
//                .build();
//        recordRepository.save(record1);
//
//
//        // When
//        RecordResponse data = recordController.getRecordById(token, record1.getId()).getBody().getData();
//
//        // Then
//        assertNotNull(data);
//        assertEquals("test comment", data.getComment());
//    }
//
//    @Test
//    void updateRecord() {
//        // Given
//        Record record = Record.builder()
//                .project(testProject)
//                .recordStatus(RecordStatus.STARTED)
//                .tags(List.of("tag1", "tag2", "tag3"))
//                .comment("test comment")
//                .build();
//
//        recordRepository.save(record);
//
//        // When
//        UpdateRecordRequest request = UpdateRecordRequest.builder()
//                .recordId(record.getId())
//                .comment("updated comment")
//                .tags(new ArrayList<>(List.of("updated tag1")))
//                .recordStatus(RecordStatus.ALMOST_DONE)
//                .build();
//
//        RecordResponse response = recordController.updateRecord(token, request).getBody().getData();
//        Record updatedRecord = recordRepository.findById(record.getId())
//                .orElseThrow(() -> new RuntimeException("record not found"));
//
//        // Then
//        assertEquals("updated comment", updatedRecord.getComment());
//        assertEquals(1, updatedRecord.getTags().size());
//
//        System.out.println("updatedRecord = " + updatedRecord.toString());
//        System.out.println("response = " + response.toString());
//    }
//
//    // 사용자 소유가 아닌 Project의 Record 변경
//    @Test
//    void updateRecord_NotAuthorized() {
//        // Given
//        Record record1 = Record.builder()
//                .project(testProject)
//                .recordStatus(RecordStatus.STARTED)
//                .tags(List.of("tag1", "tag2", "tag3"))
//                .comment("test comment")
//                .build();
//        recordRepository.save(record1);
//
//        User testUser2 = User.builder()
//                .kakaoId(2L)
//                .nickname("test user 2")
//                .build();
//
//        userRepository.save(testUser2);
//
//        Project testProject2 = Project.builder()
//                .user(testUser2)
//                .design(testDesign)
//                .nickname("test project for user 2")
//                .build();
//        projectRepository.save(testProject2);
//
//        Record record2 = Record.builder()
//                .project(testProject2)
//                .recordStatus(RecordStatus.STARTED)
//                .tags(List.of("tag1", "tag2", "tag3"))
//                .comment("test comment for user2")
//                .build();
//        recordRepository.save(record2);
//
//        // When
//        UpdateRecordRequest request = UpdateRecordRequest.builder()
//                .recordId(record2.getId())  // testUser2의 record를 수정 시도
//                .comment("updated comment")
//                .recordStatus(RecordStatus.ALMOST_DONE)
//                .build();
//
//        // Then
//        CustomException exception = assertThrows(CustomException.class, () -> {
//            recordController.updateRecord(token, request);
//        });
//
//        assertEquals(ApiResponseCode.FORBIDDEN_ACCESS, exception.getCode());
//    }
//
//    @Test
//    void deleteRecordById() {
//        // Given
//        Record record = Record.builder()
//                .project(testProject)
//                .recordStatus(RecordStatus.STARTED)
//                .tags(List.of("tag1", "tag2", "tag3"))
//                .comment("test comment")
//                .build();
//
//        recordRepository.save(record);
//        List<Record> records = recordRepository.findAllByUserId(testUser.getId());
//        assertEquals(1, records.size());
//
//        // When
//        recordController.deleteRecordById(token, record.getId());
//
//        // Then
//        List<Record> afterRecords = recordRepository.findAllByUserId(testUser.getId());
//        assertEquals(0, afterRecords.size());
//        CustomException customException = assertThrows(CustomException.class, () -> {
//            recordController.deleteRecordById(token, record.getId());
//        });
//
//        assertEquals(ApiResponseCode.RECORD_NOT_FOUND, customException.getCode());
//    }
//
//    @Test
//    void deleteRecordsWhenProjectDeleted() {
//        // Given
//        Record record1 = Record.builder()
//                .project(testProject)
//                .recordStatus(RecordStatus.STARTED)
//                .tags(List.of("tag1", "tag2", "tag3"))
//                .comment("test comment")
//                .build();
//
//        Record record2 = Record.builder()
//                .project(testProject)
//                .recordStatus(RecordStatus.STARTED)
//                .tags(List.of("tag1", "tag2", "tag3"))
//                .comment("test comment")
//                .build();
//
//        recordRepository.save(record1);
//        recordRepository.save(record2);
//
//        // When
//        projectService.deleteProject(token, testProject.getId());
//
//        // Then
//        CustomException customException = assertThrows(CustomException.class, () -> {
//            recordController.getRecordsByProjectId(token, testProject.getId());
//        });
//
//        assertEquals(ApiResponseCode.PROJECT_NOT_FOUND, customException.getCode());
//
//        assertThrows(RuntimeException.class, () -> {
//            recordRepository.findById(record1.getId())
//                    .orElseThrow(() -> new RuntimeException("Record not found"));
//        });
//
//    }
//}