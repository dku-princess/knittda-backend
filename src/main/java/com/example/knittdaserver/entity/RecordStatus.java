package com.example.knittdaserver.entity;

import com.example.knittdaserver.common.response.ApiResponseCode;
import com.example.knittdaserver.common.response.CustomException;


public enum RecordStatus {
    NOT_STARTED(0),
    STARTED(25),
    IN_PROGRESS(50),
    ALMOST_DONE(75),
    COMPLETED(100);

    private final int progress;

    RecordStatus(int progress) {
        this.progress = progress;
    }

    public int getProgress() {
        return progress;
    }

    public static RecordStatus fromString(String value) {
        for (RecordStatus status : RecordStatus.values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new CustomException(ApiResponseCode.INVALID_INPUT);
    }
}
