package com.example.knittdaserver.repository;

import com.example.knittdaserver.dto.DesignDto;
import com.example.knittdaserver.entity.QDesign;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@RequiredArgsConstructor
@Repository
public class DesignRepositoryImpl implements DesignRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<DesignDto> searchByKeyword(List<String> keywords) {
        QDesign design = QDesign.design;
        BooleanBuilder baseCondition = new BooleanBuilder();
        BooleanBuilder keywordCondition = new BooleanBuilder();
        CaseBuilder caseBuilder = new CaseBuilder();

        // 기본 조건: visible이 true인 도안만 조회
        baseCondition.and(design.visible.isTrue());

        // 키워드 조건
        for (String keyword : keywords) {
            keywordCondition.or(design.title.contains(keyword))
                    .or(design.designer.contains(keyword));
        }

        NumberExpression<Integer> score = Expressions.numberTemplate(Integer.class, "0");
        for (String keyword : keywords) {
            score = score.add(
                    caseBuilder
                            .when(design.title.contains(keyword).or(design.designer.contains(keyword)))
                            .then(1)
                            .otherwise(0)
            );
        }

        return queryFactory
                .select(
                        Projections.constructor(
                                DesignDto.class,
                                design.id,
                                design.title,
                                design.designer,
                                design.price,
                                design.imageUrl,
                                design.detailUrl,
                                design.categories,
                                design.tools,
                                design.sizes,
                                design.gauge,
                                design.needles,
                                design.yarnInfo,
                                design.pages,
                                design.visible
                        )
                )
                .distinct()
                .from(design)
                .where(baseCondition.and(keywordCondition))
                .orderBy(score.desc())
                .fetch();
    }

}
