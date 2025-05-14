package com.example.knittdaserver.repository;

import com.example.knittdaserver.dto.DesignDto;
import com.example.knittdaserver.entity.Design;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DesignRepository extends JpaRepository<Design, Long>, DesignRepositoryCustom {

    @Query("""
            select new com.example.knittdaserver.dto.DesignDto(
                d.id,
                d.title,
                d.designer,
                d.price,
                d.imageUrl,
                d.detailUrl,
                d.categories,
                d.tools,
                d.sizes,
                d.gauge,
                d.needles,
                d.yarnInfo,
                d.pages
            )
            from Design d where d.title like %:title%
            """)
    List<DesignDto> searchByTitle(@Param("title") String title);

    @Query("""
            select new com.example.knittdaserver.dto.DesignDto(
                d.id,
                d.title,
                d.designer,
                d.price,
                d.imageUrl,
                d.detailUrl,
                d.categories,
                d.tools,
                d.sizes,
                d.gauge,
                d.needles,
                d.yarnInfo,
                d.pages
            )
            from Design d where d.designer like %:designer%
            """)
    List<DesignDto> searchByDesigner(@Param("designer") String designer);

    @Query("""
            select new com.example.knittdaserver.dto.DesignDto(
                d.id,
                d.title,
                d.designer,
                d.price,
                d.imageUrl,
                d.detailUrl,
                d.categories,
                d.tools,
                d.sizes,
                d.gauge,
                d.needles,
                d.yarnInfo,
                d.pages
            )
            from Design d where d.title like %:title% and d.designer like %:designer%
            """)
    List<DesignDto> searchByTitleAndDesigner(@Param("title") String title, @Param("designer") String designer);

    @Query("""
            select new com.example.knittdaserver.dto.DesignDto(
                d.id,
                d.title,
                d.designer,
                d.price,
                d.imageUrl,
                d.detailUrl,
                d.categories,
                d.tools,
                d.sizes,
                d.gauge,
                d.needles,
                d.yarnInfo,
                d.pages
            )
            from Design d
            """)
    List<DesignDto> searchAll();

    @Query("""
            select new com.example.knittdaserver.dto.DesignDto(
                d.id,
                d.title,
                d.designer,
                d.price,
                d.imageUrl,
                d.detailUrl,
                d.categories,
                d.tools,
                d.sizes,
                d.gauge,
                d.needles,
                d.yarnInfo,
                d.pages
            )
            from Design d where (d.title like %:keyword%) or (d.designer like %:keyword%)
            """)
    List<DesignDto> searchSingleKeyword(@Param("keyword") String keyword);

    @Query("""
            select distinct new com.example.knittdaserver.dto.DesignDto(
                d.id,
                d.title,
                d.designer,
                d.price,
                d.imageUrl,
                d.detailUrl,
                d.categories,
                d.tools,
                d.sizes,
                d.gauge,
                d.needles,
                d.yarnInfo,
                d.pages
            )
            from Design d
            where d.title like %:kw1% or d.designer like %:kw1% or d.title like %:kw2% or d.designer like %:kw2%
            order by 
                case
                    when (d.title like %:kw1% and d.designer like %:kw2%) or (d.title like %:kw2% and d.designer like %:kw1%) then 1
                    when (d.title like %:kw1% or d.title like %:kw2%) or (d.designer like %:kw1% or d.designer like %:kw2%) then 2
                    else 3
                end            
            """)
    List<DesignDto> searchTwoKeywords(@Param("kw1") String kw1, @Param("kw2") String kw2);

    Design findByTitle(String title);
}

