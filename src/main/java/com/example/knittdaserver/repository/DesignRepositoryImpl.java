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

import java.util.List;

@RequiredArgsConstructor
public class DesignRepositoryImpl implements DesignRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public List<DesignDto> searchByKeyword(List<String> keywords) {
        QDesign design = QDesign.design;
        BooleanBuilder builder = new BooleanBuilder();
        CaseBuilder caseBuilder = new CaseBuilder();

        for (String keyword : keywords) {
            builder.or(design.title.contains(keyword))
                    .or(design.designer.contains(keyword));
        }

        // CASE WHEN으로 매칭된 키워드 수 계산
        NumberExpression<Integer> score = Expressions.numberTemplate(Integer.class, "0");
        for (String keyword : keywords) {
            score = score.add(
                    caseBuilder
                            .when(design.title.contains(keyword))
                            .then(1)
                            .otherwise(0)
            ).add(
                    caseBuilder
                            .when(design.designer.contains(keyword))
                            .then(1)
                            .otherwise(0)
            );
        }

        return queryFactory
                .select(
                        Projections.constructor(
                                DesignDto.class,
                                design.title,
                                design.designer,
                                design.needles,
                                design.yarnInfo)
                )
                .distinct()
                .from(design)
                .where(builder)
                .orderBy(score.desc())
                .fetch();
    }
}
