package com.example.knittdaserver.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDesign is a Querydsl query type for Design
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDesign extends EntityPathBase<Design> {

    private static final long serialVersionUID = 110850640L;

    public static final QDesign design = new QDesign("design");

    public final StringPath categories = createString("categories");

    public final StringPath designer = createString("designer");

    public final StringPath detailUrl = createString("detailUrl");

    public final StringPath gauge = createString("gauge");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imageUrl = createString("imageUrl");

    public final StringPath needles = createString("needles");

    public final StringPath pages = createString("pages");

    public final StringPath price = createString("price");

    public final StringPath sizes = createString("sizes");

    public final StringPath title = createString("title");

    public final StringPath tools = createString("tools");

    public final BooleanPath visible = createBoolean("visible");

    public final StringPath yarnInfo = createString("yarnInfo");

    public QDesign(String variable) {
        super(Design.class, forVariable(variable));
    }

    public QDesign(Path<? extends Design> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDesign(PathMetadata metadata) {
        super(Design.class, metadata);
    }

}

