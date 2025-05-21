package com.example.knittdaserver.entity;

public enum ProjectStatus {
//    NOT_STARTED("아직 안 떴어요"),
    IN_PROGRESS("뜨고 있어요"),
//    STOPPED("쉬는 중이에요"),
    DONE("다 떴어요");

    private final String message;

    ProjectStatus(String message) {
        this.message = message;
    }
    public String getMessage() {
        return message;
    }
}
