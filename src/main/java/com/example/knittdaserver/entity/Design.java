package com.example.knittdaserver.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.ToString;

@Entity
@Getter
@ToString
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

    @Lob
    private String sizes;

    @Lob
    private String gauge;

    @Lob
    private String needles;

    @Lob
    @Column(name = "yarn_info", columnDefinition = "TEXT")
    private String yarnInfo;

    @Size(max = 50)
    private String pages;

}
