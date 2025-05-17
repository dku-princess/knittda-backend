package com.example.knittdaserver.dto;

import com.example.knittdaserver.entity.Design;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class CreateDesignRequest {

    @NotBlank(message = "도안 제목은 필수입니다.")
    private String title;
    private String designer;
    private String price;
    private String imageUrl;
    private String detailUrl;
    private String categories;
    private String tools;
    private String sizes;
    private String gauge;
    private String needles;
    private String yarnInfo;
    private String pages;
    private boolean visible;

    public Design to(){
        return Design.builder()
                .title(title)
                .designer(designer)
                .price(price)
                .imageUrl(imageUrl)
                .detailUrl(detailUrl)
                .categories(categories)
                .tools(tools)
                .sizes(sizes)
                .gauge(gauge)
                .needles(needles)
                .yarnInfo(yarnInfo)
                .pages(pages)
                .visible(visible)
                .build();
        }
}
