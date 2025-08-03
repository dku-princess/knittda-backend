package com.example.knittdaserver.controller;

import com.example.knittdaserver.common.response.ApiResponse;
import com.example.knittdaserver.dto.AuthResponse;
import com.example.knittdaserver.dto.UserDto;
import com.example.knittdaserver.dto.UserResponse;
import com.example.knittdaserver.service.AuthService;
import com.example.knittdaserver.util.JwtUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "인증 관련 API")
public class AuthController {

    private final AuthService authService;

    /**
     * 카카오 로그인 API
     *
     * @param token - Bearer Token (JWT)
     * @return AuthResponse
     */
    @Operation(
            summary = "카카오 로그인",
            description = "카카오 계정을 통해 로그인합니다.",
            parameters = {
                    @Parameter(
                            name = "Authorization",
                            description = "Bearer Token (예: Bearer {token})",
                            required = true
                    )
            },
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "로그인 성공",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AuthResponse.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    )
            }
    )
    @GetMapping(value = "/kakao", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<AuthResponse>> loginKakao(
            @RequestHeader(name = "Authorization") String token) {

        String accessToken = token.replace("Bearer ", "");
        AuthResponse authResponse = authService.loginWithKakao(accessToken);
        log.info(authResponse.toString());
        return ResponseEntity.ok(ApiResponse.success(authResponse));
    }

    /**
     * JWT 토큰을 통해 사용자 정보 조회
     *
     * @param token - Bearer Token (JWT)
     * @return UserResponse
     */
    @Operation(
            summary = "JWT 사용자 정보 조회",
            description = "JWT 토큰을 통해 사용자 정보를 조회합니다.",
            parameters = {
                    @Parameter(
                            name = "Authorization",
                            description = "Bearer Token (예: Bearer {token})",
                            required = true
                    )
            },
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "사용자 정보 조회 성공",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserDto.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "401",
                            description = "인증 실패"
                    )
            }
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getUserInfoFromJwt(
            @RequestHeader(name = "Authorization") String token) {

        String accessToken = token.replace("Bearer ", "");
        UserResponse user = authService.getUserResponseFromJwt(accessToken);
        log.info(user.toString());
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @DeleteMapping("/signout")
    public ResponseEntity<ApiResponse<Void>> signout(
            @RequestHeader(name = "Authorization") String token) {

        String accessToken = token.replace("Bearer ", "");
        authService.deleteUser(accessToken);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

}
