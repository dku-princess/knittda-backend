package com.example.knittdaserver.service;

import com.example.knittdaserver.common.metrics.BusinessMetrics;
import com.example.knittdaserver.common.response.ApiResponseCode;
import com.example.knittdaserver.common.response.CustomException;
import com.example.knittdaserver.dto.AuthResponse;
import com.example.knittdaserver.dto.KakaoUserResponse;
import com.example.knittdaserver.dto.UpdateNicknameRequest;
import com.example.knittdaserver.dto.UserDto;
import com.example.knittdaserver.dto.UserResponse;
import com.example.knittdaserver.entity.Project;
import com.example.knittdaserver.entity.User;
import com.example.knittdaserver.repository.ProjectRepository;
import com.example.knittdaserver.repository.UserRepository;
import com.example.knittdaserver.util.JwtUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final ProjectRepository projectRepository;
    private final WebClient.Builder webClientBuilder;
    private final FirebaseAuth firebaseAuth;
    private final S3Service s3Service;
    private final BusinessMetrics businessMetrics;

    private final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    @Transactional
    public AuthResponse loginWithKakao(String kakaoAccessToken) {

        UserDto userDto = getKakaoUserInfo(kakaoAccessToken);
        Optional<User> userOptional = userRepository.findByKakaoId(userDto.getKakaoId());
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
        } else {
            user = User.builder()
                    .kakaoId(userDto.getKakaoId())
                    .nickname(userDto.getNickname())
                    .email(userDto.getEmail())
                    .profileImageUrl(userDto.getProfileImageUrl())
                    .build();
            userRepository.save(user);
            businessMetrics.count("user.signup", "provider", "kakao");
        }

        String jwt = jwtUtil.generateToken(user.getId());
        userDto = UserDto.from(user);
        businessMetrics.count("auth.login", "method", "kakao");
        return new AuthResponse(jwt, userDto);
    }

    @Transactional
    public AuthResponse loginWithApple(String appleAccessToken, String name) {
        UserDto userDto = getAppleUserInfo(appleAccessToken);
        Optional<User> userOptional = userRepository.findByAppleId(userDto.getAppleId());
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
        } else {
            user = User.builder()
                    .appleId(userDto.getAppleId())
                    .nickname(name.isBlank() ? null : name)
                    .email(userDto.getEmail())
                    .profileImageUrl(userDto.getProfileImageUrl())
                    .build();
            userRepository.save(user);
            businessMetrics.count("user.signup", "provider", "apple");
        }

        String jwt = jwtUtil.generateToken(user.getId());
        userDto = UserDto.from(user);
        businessMetrics.count("auth.login", "method", "apple");
        return new AuthResponse(jwt, userDto);
    }

    // AppStore 심사 시 사용하는 로그인 메서드
    @Transactional
    public AuthResponse loginForAdmin() {

        Long kakaoId = 4250638508L;
        User user = userRepository.findByKakaoId(kakaoId)
        .orElse(
            User.builder()
                .kakaoId(kakaoId)
                .nickname("admin")
                .email("admin@admin.com")
                .build()
        );
        userRepository.save(user);
        String jwt = jwtUtil.generateToken(user.getId());
        UserDto userDto = UserDto.from(user);
        return new AuthResponse(jwt, userDto);
    }


    public UserDto getKakaoUserInfo(String kakaoAccessToken) {

        var headers = new HttpHeaders();
        headers.setBearerAuth(kakaoAccessToken);

        try {
            KakaoUserResponse kakaoUserResponse = webClientBuilder.build()
                    .get()
                    .uri(KAKAO_USER_INFO_URL)
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .retrieve()
                    .bodyToMono(KakaoUserResponse.class)
                    .block();

            if (kakaoUserResponse == null) {
                throw new CustomException(ApiResponseCode.USER_NOT_FOUND);
            }

            return UserDto.builder()
                    .kakaoId(kakaoUserResponse.getId())
                    .nickname(kakaoUserResponse.getKakao_account().getProfile().getNickname())
                    .email(kakaoUserResponse.getKakao_account().getEmail())
                    .profileImageUrl(kakaoUserResponse.getKakao_account().getProfile().getProfile_image_url())
                    .build();

        } catch (WebClientResponseException e) {
            return failFromKakaoWebClient(e);
        } catch (Exception e) {
            WebClientResponseException nested = findNestedWebClientResponseException(e);
            if (nested != null) {
                return failFromKakaoWebClient(nested);
            }
            throw new CustomException(ApiResponseCode.SERVER_ERROR);
        }
    }

    private static WebClientResponseException findNestedWebClientResponseException(Throwable throwable) {
        Throwable t = throwable;
        for (int depth = 0; depth < 8 && t != null; depth++) {
            if (t instanceof WebClientResponseException wcre) {
                return wcre;
            }
            t = t.getCause();
        }
        return null;
    }

    private UserDto failFromKakaoWebClient(WebClientResponseException e) {
        if (e.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()) {
            throw new CustomException(ApiResponseCode.INVALID_TOKEN);
        }
        if (e.getStatusCode().value() == HttpStatus.FORBIDDEN.value()) {
            throw new CustomException(ApiResponseCode.INVALID_TOKEN);
        }
        if (e.getStatusCode().is5xxServerError()) {
            throw new CustomException(ApiResponseCode.SERVER_ERROR);
        }
        throw new CustomException(ApiResponseCode.INVALID_TOKEN);
    }

    public UserDto getAppleUserInfo(String appleIdToken) {
        try {
            // Firebase Admin SDK를 사용하여 Apple ID 토큰 검증
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(appleIdToken);

            // 검증된 토큰에서 사용자 정보 추출
            return UserDto.builder()
                    .appleId(decodedToken.getUid())
                    .email(decodedToken.getEmail())
                    .nickname(decodedToken.getName())
                    .profileImageUrl(decodedToken.getPicture())
                    .build();

        } catch (FirebaseAuthException e) {
            log.error("Firebase token verification failed", e);
            throw new CustomException(ApiResponseCode.INVALID_TOKEN);
        } catch (Exception e) {
            log.error("Failed to get Apple user info", e);
            throw new CustomException(ApiResponseCode.SERVER_ERROR);
        }
    }

    public UserResponse getUserResponseFromJwt(String jwt) {
        try {
            Long userId = jwtUtil.validateAndExtractUserId(jwt);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ApiResponseCode.USER_NOT_FOUND));
            // 토큰 기반 자동 로그인(/me = AutoLoginUseCase). 토큰 검증 성공 지점에서만 계측.
            businessMetrics.count("auth.login", "method", "auto");
            return UserResponse.from(user);
        }catch (Exception e) {
            throw new CustomException(ApiResponseCode.INVALID_TOKEN);
        }
    }

    public User getUserFromJwt(String token) {
        try {
            String jwt = token.replace("Bearer ", "");
            Long userId = jwtUtil.validateAndExtractUserId(jwt);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ApiResponseCode.USER_NOT_FOUND));
            return user;
        } catch (Exception e) {
            log.error("getUserDtoFromJwt error", e);
            throw new CustomException(ApiResponseCode.INVALID_TOKEN);
        }
    }


    @Transactional
    public void deleteUser(String jwt) {
        Long userId = jwtUtil.validateAndExtractUserId(jwt);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ApiResponseCode.USER_NOT_FOUND));
        List<Project> projects = projectRepository.findByUserId(userId);
        projectRepository.deleteAll(projects);
        userRepository.delete(user);
    }

    @Transactional
    public UserResponse updateNickname(String tokenHeader, UpdateNicknameRequest request) {
        if (request == null) {
            throw new CustomException(ApiResponseCode.INVALID_INPUT);
        }
        if (request.getNickname() == null || request.getNickname().isBlank()) {
            throw new CustomException(ApiResponseCode.INVALID_INPUT);
        }
        if (request.getUserId() == null) {
            throw new CustomException(ApiResponseCode.INVALID_INPUT);
        }

        String jwt = tokenHeader.replace("Bearer ", "");
        Long userId = jwtUtil.validateAndExtractUserId(jwt);
        if (!userId.equals(request.getUserId())) {
            throw new CustomException(ApiResponseCode.FORBIDDEN_ACCESS);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ApiResponseCode.USER_NOT_FOUND));
        user.setNickname(request.getNickname().trim());
        userRepository.save(user);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse uploadProfileImage(String jwt, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ApiResponseCode.INVALID_INPUT);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new CustomException(ApiResponseCode.INVALID_INPUT);
        }

        String token = jwt.replace("Bearer ", "");
        Long userId = jwtUtil.validateAndExtractUserId(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ApiResponseCode.USER_NOT_FOUND));

        File tempFile = null;
        try {
            String original = file.getOriginalFilename();
            String suffix = (original != null && original.contains("."))
                    ? original.substring(original.lastIndexOf('.'))
                    : ".tmp";
            tempFile = File.createTempFile("profile_upload_", suffix);
            file.transferTo(tempFile);

            String newUrl = s3Service.uploadProfileImage(tempFile);

            String prevUrl = user.getProfileImageUrl();
            if (prevUrl != null && !prevUrl.isBlank()) {
                try {
                    s3Service.deleteFile(prevUrl);
                } catch (Exception e) {
                    log.warn("[AuthService] 기존 이미지 S3 삭제 실패 (무시) - url: {}, 에러: {}", prevUrl, e.getMessage());
                }
            }

            user.setProfileImageUrl(newUrl);
            userRepository.save(user);
            return UserResponse.from(user);
        } catch (IOException e) {
            log.error("[AuthService] 이미지 업로드 실패 - userId: {}", userId, e);
            throw new CustomException(ApiResponseCode.IMAGE_UPLOAD_FAILED);
        } finally {
            if (tempFile != null && tempFile.exists() && !tempFile.delete()) {
                tempFile.deleteOnExit();
            }
        }
    }

}
