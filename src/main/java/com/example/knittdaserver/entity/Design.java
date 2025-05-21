package com.example.knittdaserver.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Design {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max=255)
    private String title;

    @Size(max=255)
    private String designer;

    @Size(max = 50)
    @Column(name = "price", length = 50)
    private String price;

    @Size(max = 500)
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Size(max = 500)
    @Column(name = "detail_url", length = 500, unique = true)
    private String detailUrl;

    @Size(max = 255)
    private String categories;

    @Size(max = 255)
    private String tools;

    @Size(max = 255)
    private String sizes;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String gauge;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String needles;

    @Lob
    @Column(name = "yarn_info", columnDefinition = "TEXT")
    private String yarnInfo;

    @Size(max = 50)
    @Column(columnDefinition = "TEXT")
    private String pages;

    @Builder.Default
    private boolean visible = false;


}
