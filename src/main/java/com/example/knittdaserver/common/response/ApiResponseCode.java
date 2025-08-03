package com.example.knittdaserver.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ApiResponseCode {
    // 성공
    SUCCESS("S200", "요청에 성공했습니다", HttpStatus.OK),

    // 인증 / 권한 관련
    UNAUTHORIZED("A401", "로그인이 필요합니다", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("A402", "유효하지 않은 토큰입니다", HttpStatus.UNAUTHORIZED),
    FORBIDDEN_ACCESS("A403", "접근 권한이 없습니다", HttpStatus.FORBIDDEN),

    // 사용자 관련
    USER_NOT_FOUND("U404", "사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    EMAIL_DUPLICATED("U001", "이미 사용 중인 이메일입니다", HttpStatus.CONFLICT),

    // 프로젝트 관련
    PROJECT_NOT_FOUND("P404", "프로젝트를 찾을 수 없습니다", HttpStatus.NOT_FOUND),

    // 기록 관련
    RECORD_NOT_FOUND("R404", "기록을 찾을 수 없습니다", HttpStatus.NOT_FOUND),

    // 도안 관련
    DESIGN_NOT_FOUND("D404", "도안을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    INVALID_DESIGN_INFO("D400","유효하지 않은 도안 정보입니다.",HttpStatus.BAD_REQUEST),


    // 입력 관련
    INVALID_INPUT("C001", "입력값이 올바르지 않습니다", HttpStatus.BAD_REQUEST),

    // 서버 오류
    SERVER_ERROR("E500", "서버 내부 오류입니다", HttpStatus.INTERNAL_SERVER_ERROR),

    // 이미지 업로드 관련
    IMAGE_UPLOAD_FAILED("I001", "이미지 업로드에 실패했습니다", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
