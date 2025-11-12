package com.example.knittdaserver.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QDesign is a Querydsl query type for Design
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDesign extends EntityPathBase<Design> {

    private static final long serialVersionUID = 110850640L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QDesign design = new QDesign("design");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath description = createString("description");

    public final StringPath designer = createString("designer");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath needleInfo = createString("needleInfo");

    public final QProject project;

    public final StringPath title = createString("title");

    public final BooleanPath visible = createBoolean("visible");

    public final StringPath yarnInfo = createString("yarnInfo");

    public QDesign(String variable) {
        this(Design.class, forVariable(variable), INITS);
    }

    public QDesign(Path<? extends Design> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QDesign(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QDesign(PathMetadata metadata, PathInits inits) {
        this(Design.class, metadata, inits);
    }

    public QDesign(Class<? extends Design> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.project = inits.isInitialized("project") ? new QProject(forProperty("project"), inits.get("project")) : null;
    }

}

