package com.example.knittdaserver.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ImageDto {
    private Long id;
    private String imageUrl;
    private Long imageOrder;

    public static ImageDto from(Image image) {
        if (image == null) {
            return null;
        }
        return ImageDto.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .imageOrder(image.getImageOrder())
                .build();
    }
}
