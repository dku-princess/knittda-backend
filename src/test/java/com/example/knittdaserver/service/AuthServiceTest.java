package com.example.knittdaserver.service;

import com.example.knittdaserver.common.response.ApiResponseCode;
import com.example.knittdaserver.common.response.CustomException;
import com.example.knittdaserver.dto.UpdateNicknameRequest;
import com.example.knittdaserver.dto.UserResponse;
import com.example.knittdaserver.entity.User;
import com.example.knittdaserver.repository.ProjectRepository;
import com.example.knittdaserver.repository.UserRepository;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.example.knittdaserver.util.JwtUtil;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private FirebaseAuth firebaseAuth;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("닉네임 생성/수정 - 정상 케이스")
    void updateNickname_success() {
        String tokenHeader = "Bearer jwt-token";
        String jwt = "jwt-token";
        Long userId = 1L;

        when(jwtUtil.validateAndExtractUserId(jwt)).thenReturn(userId);

        User user = User.builder()
                .id(userId)
                .nickname("oldNickname")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UpdateNicknameRequest request = UpdateNicknameRequest.builder()
                .userId(userId)
                .nickname("newNickname")
                .build();

        UserResponse response = authService.updateNickname(tokenHeader, request);

        assertEquals("newNickname", user.getNickname());
        assertEquals("newNickname", response.getNickname());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("닉네임 생성/수정 - 공백 닉네임이면 INVALID_INPUT")
    void updateNickname_blankNickname_throwsInvalidInput() {
        String tokenHeader = "Bearer jwt-token";

        UpdateNicknameRequest request = UpdateNicknameRequest.builder()
                .userId(1L)
                .nickname("   ")
                .build();

        CustomException exception = assertThrows(CustomException.class,
                () -> authService.updateNickname(tokenHeader, request));

        assertEquals(ApiResponseCode.INVALID_INPUT, exception.getCode());
    }

    @Test
    @DisplayName("닉네임 생성/수정 - 요청 userId와 토큰 유저 불일치 시 FORBIDDEN_ACCESS")
    void updateNickname_userIdMismatch_throwsForbiddenAccess() {
        String tokenHeader = "Bearer jwt-token";
        String jwt = "jwt-token";
        Long userId = 1L;

        when(jwtUtil.validateAndExtractUserId(jwt)).thenReturn(userId);

        UpdateNicknameRequest request = UpdateNicknameRequest.builder()
                .userId(999L)
                .nickname("newNickname")
                .build();

        CustomException exception = assertThrows(CustomException.class,
                () -> authService.updateNickname(tokenHeader, request));

        assertEquals(ApiResponseCode.FORBIDDEN_ACCESS, exception.getCode());
    }

    @Test
    @DisplayName("프로필 이미지 업로드 - 정상 케이스 (이전 이미지 삭제 포함)")
    void uploadProfileImage_success_withPreviousImageDelete() throws IOException {
        String jwt = "jwt-token";
        Long userId = 1L;

        when(jwtUtil.validateAndExtractUserId(jwt)).thenReturn(userId);

        User user = User.builder()
                .id(userId)
                .profileImageUrl("https://bucket.s3.amazonaws.com/profiles/old-image.webp")
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        MockMultipartFile multipartFile = new MockMultipartFile(
                "image",
                "profile.webp",
                "image/webp",
                "dummy image content".getBytes()
        );

        when(s3Service.uploadProfileImage(org.mockito.Mockito.any())).thenReturn("https://bucket.s3.amazonaws.com/profiles/new-image.webp");

        UserResponse response = authService.uploadProfileImage(jwt, multipartFile);

        assertEquals("https://bucket.s3.amazonaws.com/profiles/new-image.webp", user.getProfileImageUrl());
        assertEquals(user.getProfileImageUrl(), response.getProfileImageUrl());

        verify(s3Service).uploadProfileImage(org.mockito.Mockito.any());
        verify(s3Service).deleteFile("https://bucket.s3.amazonaws.com/profiles/old-image.webp");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("프로필 이미지 업로드 - 파일이 비어 있으면 INVALID_INPUT")
    void uploadProfileImage_emptyFile_throwsInvalidInput() {
        String jwt = "jwt-token";
        MockMultipartFile emptyFile = new MockMultipartFile(
                "image",
                "empty.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[0]
        );

        CustomException exception = assertThrows(CustomException.class,
                () -> authService.uploadProfileImage(jwt, emptyFile));

        assertEquals(ApiResponseCode.INVALID_INPUT, exception.getCode());
        verify(jwtUtil, never()).validateAndExtractUserId(jwt);
    }

    @Test
    @DisplayName("프로필 이미지 업로드 - 이미지가 아닌 MIME 타입이면 INVALID_INPUT")
    void uploadProfileImage_nonImageContentType_throwsInvalidInput() {
        String jwt = "jwt-token";
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "text.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "not image".getBytes()
        );

        CustomException exception = assertThrows(CustomException.class,
                () -> authService.uploadProfileImage(jwt, file));

        assertEquals(ApiResponseCode.INVALID_INPUT, exception.getCode());
        verify(jwtUtil, never()).validateAndExtractUserId(jwt);
    }
}

/**
 * 실제 S3 버킷에 profiles/ 키로 올린 뒤, 동일 키로 객체를 읽을 수 있는지 검증한다.
 * AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY가 없으면 테스트를 건너뛴다.
 * (로컬/CI에서 자격 증명을 넣고 확인할 때 사용)
 */
@Tag("integration")
class S3ProfileImageBucketIntegrationTest {

    private AmazonS3 amazonS3;
    private String bucketName;

    @BeforeEach
    void setUp() {
        String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
        String secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        Assumptions.assumeTrue(
                accessKey != null && !accessKey.isBlank(),
                "AWS_ACCESS_KEY_ID 가 없어 S3 통합 검증을 건너뜁니다."
        );
        Assumptions.assumeTrue(
                secretKey != null && !secretKey.isBlank(),
                "AWS_SECRET_ACCESS_KEY 가 없어 S3 통합 검증을 건너뜁니다."
        );
        String region = Optional.ofNullable(System.getenv("AWS_REGION")).orElse("ap-northeast-2");
        bucketName = Optional.ofNullable(System.getenv("AWS_S3_BUCKET")).orElse("knittda-bucket");

        amazonS3 = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials(accessKey, secretKey)
                ))
                .withRegion(region)
                .build();
    }

    @Test
    @DisplayName("S3: profiles/ 경로로 업로드한 객체를 버킷에서 읽을 수 있다 (S3Service.uploadProfileImage)")
    void uploadProfileImage_thenObjectIsReadableFromBucket() throws IOException {
        S3Service s3Service = new S3Service(amazonS3);
        ReflectionTestUtils.setField(s3Service, "bucketName", bucketName);

        File tempFile = File.createTempFile("profile-integration-", ".webp");
        String objectKey = null;
        try {
            Files.writeString(tempFile.toPath(), "integration-test-bytes", StandardCharsets.UTF_8);

            String publicUrl = s3Service.uploadProfileImage(tempFile);
            assertNotNull(publicUrl);
            assertTrue(publicUrl.contains("profiles/"), "URL에 profiles/ 경로가 포함되어야 함");

            objectKey = URI.create(publicUrl).getPath().replaceFirst("^/", "");
            assertTrue(
                    amazonS3.doesObjectExist(bucketName, objectKey),
                    "업로드 직후 동일 키로 객체가 존재해야 함: " + objectKey
            );

            try (S3Object s3Object = amazonS3.getObject(bucketName, objectKey);
                 InputStream in = s3Object.getObjectContent()) {
                byte[] body = in.readAllBytes();
                assertTrue(body.length > 0, "getObject 로 바이트를 읽을 수 있어야 함");
            }
        } finally {
            if (objectKey != null) {
                try {
                    amazonS3.deleteObject(bucketName, objectKey);
                } catch (RuntimeException ignored) {
                    // 정리 실패는 테스트 결과에 영향 없음
                }
            }
            if (!tempFile.delete()) {
                tempFile.deleteOnExit();
            }
        }
    }
}

