package com.example.knittdaserver.service;

import com.example.knittdaserver.common.response.ApiResponse;
import com.example.knittdaserver.common.response.ApiResponseCode;
import com.example.knittdaserver.common.response.CustomException;
import com.example.knittdaserver.dto.AuthResponse;
import com.example.knittdaserver.dto.KakaoUserResponse;
import com.example.knittdaserver.dto.UserDto;
import com.example.knittdaserver.dto.UserResponse;
import com.example.knittdaserver.entity.User;
import com.example.knittdaserver.repository.UserRepository;
import com.example.knittdaserver.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final WebClient.Builder webClientBuilder;

    private final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    public AuthResponse loginWithKakao(String kakaoAccessToken) {

        UserDto userDto = getUserInfo(kakaoAccessToken);
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
        return new AuthResponse(jwt, userDto);
    }

    public UserDto getUserInfo(String kakaoAccessToken) {

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

    public UserResponse getUserResponseFromJwt(String jwt) {
        try {
            Long userId = jwtUtil.validateAndExtractUserId(jwt);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ApiResponseCode.USER_NOT_FOUND));
            return UserResponse.from(user);
        }catch (Exception e) {
            log.error("getUserResponseFromJwt error", e);
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


}
