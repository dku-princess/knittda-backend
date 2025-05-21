package com.example.knittdaserver.entity;

import com.example.knittdaserver.common.response.ApiResponseCode;
import com.example.knittdaserver.common.response.CustomException;
import com.fasterxml.jackson.annotation.JsonCreator;

public enum RecordStatus {
    NOT_STARTED,
    STARTED,
    IN_PROGRESS,
    ALMOST_DONE,
    COMPLETED;

    public static RecordStatus fromString(String value) {
        for (RecordStatus status : RecordStatus.values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new CustomException(ApiResponseCode.INVALID_INPUT);
    }
}
