package com.example.knittdaserver.dto;

import com.example.knittdaserver.entity.Design;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateDesignRequest {

    private String title;
    private String designer;
    private String needleInfo;
    private String yarnInfo;
    private String description;

    public Design to() {
        return Design.builder()
                .title(title)
                .designer(designer)
                .needleInfo(needleInfo)
                .yarnInfo(yarnInfo)
                .description(description)
                .build();
    }
}
