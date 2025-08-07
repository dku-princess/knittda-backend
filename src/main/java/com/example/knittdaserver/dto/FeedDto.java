package com.example.knittdaserver.dto;

import java.time.LocalDateTime;

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
    
    @Override
    public String toString() {
        return String.format("FeedDto{userName='%s', projectName='%s', designTitle='%s', designer='%s', projectId=%d, recordId=%d, similarityScore=%.2f}",
            userName, projectName, designTitle, designer, projectId, 
            record != null ? record.getId() : null, similarityScore);
    }
}
