package com.example.knittdaserver.service;

import com.example.knittdaserver.common.response.ApiResponseCode;
import com.example.knittdaserver.common.response.CustomException;
import com.example.knittdaserver.dto.AuthResponse;
import com.example.knittdaserver.dto.KakaoUserResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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
        }

        String jwt = jwtUtil.generateToken(user.getId());
        userDto = UserDto.from(user);
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
        }

        String jwt = jwtUtil.generateToken(user.getId());
        userDto = UserDto.from(user);
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

        } catch (Exception e) {
            throw new CustomException(ApiResponseCode.SERVER_ERROR);
        }
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
    
}
