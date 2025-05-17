package com.example.knittdaserver.dto;

import com.example.knittdaserver.entity.Design;
import com.example.knittdaserver.entity.Project;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
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

    public DesignDto(Long id, String title, String designer, String price, String imageUrl, String detailUrl,
                     String categories, String tools, String sizes, String gauge, String needles, String yarnInfo, String pages, boolean visible) {
        this.id = id;
        this.title = title;
        this.designer = designer;
        this.price = price;
        this.imageUrl = imageUrl;
        this.detailUrl = detailUrl;
        this.categories = categories;
        this.tools = tools;
        this.sizes = sizes;
        this.gauge = gauge;
        this.needles = needles;
        this.yarnInfo = yarnInfo;
        this.pages = pages;
        this.visible = visible;
    }
}
