package com.example.knittdaserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SearchResponse {
    private String searchId;
    private String searchVersion;
    private Long totalElements;
    private Integer totalPages;
    private Integer number;
    private Integer size;
    private java.util.List<FeedDto> content;
    
    public static SearchResponse from(Page<FeedDto> page, String searchId, String searchVersion) {
        return SearchResponse.builder()
            .searchId(searchId)
            .searchVersion(searchVersion)
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .number(page.getNumber())
            .size(page.getSize())
            .content(page.getContent())
            .build();
    }
}

