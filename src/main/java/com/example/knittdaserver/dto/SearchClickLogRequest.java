package com.example.knittdaserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SearchClickLogRequest {
    
    @NotBlank(message = "searchId는 필수입니다")
    private String searchId;
    
    @NotBlank(message = "keyword는 필수입니다")
    private String keyword;
    
    @NotNull(message = "recordId는 필수입니다")
    private Long recordId;
    
    @NotNull(message = "clickRank는 필수입니다")
    private Integer clickRank;
    
    @NotNull(message = "page는 필수입니다")
    private Integer page;
}

