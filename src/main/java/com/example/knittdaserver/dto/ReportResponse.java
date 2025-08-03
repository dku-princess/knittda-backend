package com.example.knittdaserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class ReportResponse {

    private double knittingLevel;
    private int weeklyKnittingCount;
    private int weeklyKnittingPhotoCount;
    private int weeklyProgress;
    private List<String> topTags;
    private List<Hashtag> weeklyHashtags;

    @Data
    @AllArgsConstructor
    public static class Hashtag {
        private String hashtag;
        private String description;
    }
}