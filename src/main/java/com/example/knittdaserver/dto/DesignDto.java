package com.example.knittdaserver.dto;

import lombok.Data;

@Data
public class DesignDto {
    private String title;
    private String designer;
    private String needles;
    private String yarnInfo;

    public DesignDto(String title, String designer, String needles, String yarnInfo) {
        this.title = title;
        this.designer = designer;
        this.needles = needles;
        this.yarnInfo = yarnInfo;
    }
}
