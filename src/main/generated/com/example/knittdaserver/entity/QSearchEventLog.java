package com.example.knittdaserver.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QSearchEventLog is a Querydsl query type for SearchEventLog
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSearchEventLog extends EntityPathBase<SearchEventLog> {

    private static final long serialVersionUID = -1046144348L;

    public static final QSearchEventLog searchEventLog = new QSearchEventLog("searchEventLog");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath keyword = createString("keyword");

    public final StringPath searchId = createString("searchId");

    public final StringPath searchVersion = createString("searchVersion");

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QSearchEventLog(String variable) {
        super(SearchEventLog.class, forVariable(variable));
    }

    public QSearchEventLog(Path<? extends SearchEventLog> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSearchEventLog(PathMetadata metadata) {
        super(SearchEventLog.class, metadata);
    }

}

