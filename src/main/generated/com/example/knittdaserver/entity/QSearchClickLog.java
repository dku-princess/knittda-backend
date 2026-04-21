package com.example.knittdaserver.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QSearchClickLog is a Querydsl query type for SearchClickLog
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSearchClickLog extends EntityPathBase<SearchClickLog> {

    private static final long serialVersionUID = -417810186L;

    public static final QSearchClickLog searchClickLog = new QSearchClickLog("searchClickLog");

    public final NumberPath<Integer> clickRank = createNumber("clickRank", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath keyword = createString("keyword");

    public final NumberPath<Integer> page = createNumber("page", Integer.class);

    public final NumberPath<Long> recordId = createNumber("recordId", Long.class);

    public final StringPath searchId = createString("searchId");

    public final StringPath searchVersion = createString("searchVersion");

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QSearchClickLog(String variable) {
        super(SearchClickLog.class, forVariable(variable));
    }

    public QSearchClickLog(Path<? extends SearchClickLog> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSearchClickLog(PathMetadata metadata) {
        super(SearchClickLog.class, metadata);
    }

}

