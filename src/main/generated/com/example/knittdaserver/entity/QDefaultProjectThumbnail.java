package com.example.knittdaserver.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDefaultProjectThumbnail is a Querydsl query type for DefaultProjectThumbnail
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDefaultProjectThumbnail extends EntityPathBase<DefaultProjectThumbnail> {

    private static final long serialVersionUID = -1314566174L;

    public static final QDefaultProjectThumbnail defaultProjectThumbnail = new QDefaultProjectThumbnail("defaultProjectThumbnail");

    public final BooleanPath active = createBoolean("active");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imageUrl = createString("imageUrl");

    public final StringPath name = createString("name");

    public final NumberPath<Integer> sortOrder = createNumber("sortOrder", Integer.class);

    public QDefaultProjectThumbnail(String variable) {
        super(DefaultProjectThumbnail.class, forVariable(variable));
    }

    public QDefaultProjectThumbnail(Path<? extends DefaultProjectThumbnail> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDefaultProjectThumbnail(PathMetadata metadata) {
        super(DefaultProjectThumbnail.class, metadata);
    }

}

