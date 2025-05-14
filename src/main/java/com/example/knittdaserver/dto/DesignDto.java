package com.example.knittdaserver.dto;

import com.example.knittdaserver.entity.Design;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesignDto {
    private Long id;
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

    public static DesignDto from(Design design) {
        return DesignDto.builder()
                .id(design.getId())
                .title(design.getTitle())
                .designer(design.getDesigner())
                .price(design.getPrice())
                .imageUrl(design.getImageUrl())
                .detailUrl(design.getDetailUrl())
                .categories(design.getCategories())
                .tools(design.getTools())
                .sizes(design.getSizes())
                .gauge(design.getGauge())
                .needles(design.getNeedles())
                .yarnInfo(design.getYarnInfo())
                .pages(design.getPages())
                .visible(design.isVisible())
                .build();
    }
}
