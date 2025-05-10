package com.example.knittdaserver.common.response;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
    private final ApiResponseCode code;
    public CustomException(ApiResponseCode code) {
        super(code.getMessage());
        this.code = code;
    }
}
