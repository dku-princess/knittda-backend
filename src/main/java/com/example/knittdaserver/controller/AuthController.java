package com.example.knittdaserver.controller;

import com.example.knittdaserver.common.response.ApiResponse;
import com.example.knittdaserver.dto.AuthResponse;
import com.example.knittdaserver.dto.UpdateNicknameRequest;
import com.example.knittdaserver.dto.UserDto;
import com.example.knittdaserver.dto.UserResponse;
import com.example.knittdaserver.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "인증 관련 API")
public class AuthController {

    private final AuthService authService;
    /**
     * 카카오 로그인 API
     *
     * @param token Authorization 헤더 (Bearer {카카오 액세스 토큰})
     * @return AuthResponse
     */
    @Operation(
            summary = "카카오 로그인",
            description = "카카오 OAuth 액세스 토큰으로 로그인합니다. Authorization에는 Bearer {카카오_액세스_토큰} 형식으로 전달합니다.",
            parameters = {
                    @Parameter(
                            name = "Authorization",
                            description = "Bearer {카카오 액세스 토큰}",
                            required = true
                    )
            },
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "성공 (code: S200)",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AuthResponse.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "401",
                            description = "A402 유효하지 않은 토큰 (카카오 액세스 토큰 무효·만료 등)",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiResponse.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "U404 사용자를 찾을 수 없음",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiResponse.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "500",
                            description = "E500 서버 내부 오류",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiResponse.class))
                    )
            }
    )
    @SecurityRequirements
    @GetMapping(value = "/kakao", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<AuthResponse>> loginKakao(
            @RequestHeader(name = "Authorization") String token) {
        String accessToken = token.replace("Bearer ", "");
        AuthResponse authResponse = authService.loginWithKakao(accessToken);
        return ResponseEntity.ok(ApiResponse.success(authResponse));
    }

    @Operation(
            summary = "관리자 로그인",
            description = "관리자 계정을 통해 로그인합니다.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "로그인 성공",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AuthResponse.class))
                    )})
            
    @SecurityRequirements
    @GetMapping(value = "/admin", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<AuthResponse>> loginAdmin() {
        AuthResponse authResponse = authService.loginForAdmin();
        return ResponseEntity.ok(ApiResponse.success(authResponse));
    }

    @Operation(
            summary = "애플 로그인",
            description = "Firebase로 검증 가능한 Apple ID 토큰으로 로그인합니다.",
            parameters = {
                    @Parameter(
                            name = "Authorization",
                            description = "Bearer {Apple ID 토큰}",
                            required = true
                    ),
                    @Parameter(name = "name", description = "표시 이름(선택)", required = true)
            },
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "성공 (code: S200)",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AuthResponse.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "401",
                            description = "A402 유효하지 않은 토큰 (Apple ID 토큰 검증 실패)",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiResponse.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "500",
                            description = "E500 서버 내부 오류",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiResponse.class))
                    )
            }
    )
    @SecurityRequirements
    @GetMapping(value = "/apple", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<AuthResponse>> loginApple(
            @RequestHeader(name = "Authorization") String token,
            @RequestParam(name = "name") String name) {
        String accessToken = token.replace("Bearer ", "");
        AuthResponse authResponse = authService.loginWithApple(accessToken, name);
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
            @Parameter(hidden = true) @RequestHeader(name = "Authorization") String token) {

        String accessToken = token.replace("Bearer ", "");
        UserResponse user = authService.getUserResponseFromJwt(accessToken);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @Operation(summary = "Update nickname", description = "JWT auth. JSON body: userId, nickname.")
    @PutMapping(value = "/me/nickname", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> updateNickname(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization") String token,
            @RequestBody UpdateNicknameRequest request) {
        UserResponse user = authService.updateNickname(token, request);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @Operation(summary = "Upload profile image", description = "Multipart part name: image")
    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> uploadProfileImage(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization") String token,
            @RequestPart("image") MultipartFile image) {
        String accessToken = token.replace("Bearer ", "");
        UserResponse user = authService.uploadProfileImage(accessToken, image);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @DeleteMapping("/signout")
    public ResponseEntity<ApiResponse<Void>> signout(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization") String token) {

        String accessToken = token.replace("Bearer ", "");
        authService.deleteUser(accessToken);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

}
