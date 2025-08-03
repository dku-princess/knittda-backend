package com.example.knittdaserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class FeedDto {
    
    private String userName;
    private String profileImageUrl;
    private String projectName;
    private String designTitle;
    private String designer;
    private Long projectId;
    private RecordResponse record;
    private double similarityScore;
}
