-- record.created_at 인덱스 추가
--
-- 배경: /api/v1/feed 기본 정렬(createdAt DESC)이 인덱스 없이 전체 record를 filesort하고 있었음.
--       entity/Record.java에 @Table(indexes = @Index(...))로 매핑을 추가했지만,
--       Flyway를 도입하지 않았고 prod는 ddl-auto=validate라 자동 생성되지 않으므로
--       배포 전 이 스크립트를 대상 DB에 수동으로 실행해야 한다.
--
-- 적용 대상:
--   - RDS knittda_db (prod, app-green/app-blue)  ← 반드시 수동 실행 필요
--   - RDS knittda_test (staging, app-test)        ← ddl-auto=update라 배포 시 자동 생성되지만, 먼저 수동 실행해도 무해(IF NOT EXISTS 없어 재실행 시 에러 남에 유의)
--   - local(docker-compose.yml)                    ← ddl-auto=update로 자동 생성, 실행 불필요
--
-- 실행 예:
--   mysql -h <host> -u <user> -p knittda_db < sql/2026-09-02_add_index_record_created_at.sql
--
-- 안전성 참고: record 테이블 행 수가 적어(수백 건) 즉시 완료되는 가벼운 DDL이며,
-- InnoDB는 세컨더리 인덱스 생성 시 기본적으로 온라인 DDL(ALGORITHM=INPLACE)을 사용해 잠금 영향이 작다.

CREATE INDEX idx_record_created_at ON record (created_at);
