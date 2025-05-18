package com.example.knittdaserver.common.response;

import io.sentry.Sentry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<?>> handleCustomException(CustomException ex) {
        ApiResponseCode code = ex.getCode();

        Sentry.captureException(ex);
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.fail(code));
    }
}
