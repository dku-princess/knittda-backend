package com.example.knittdaserver.common.tracing;

import io.sentry.ISpan;
import io.sentry.Sentry;
import io.sentry.SpanStatus;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 현재 Sentry 트랜잭션 아래에 child span을 붙여 요청 내부 구간(DB 조회, 매핑 등)의
 * 소요 시간을 계측하는 공통 헬퍼.
 *
 * <p>트레이싱이 샘플링되지 않은 요청은 {@link Sentry#getSpan()}이 null이라 자동으로 스킵된다.
 */
@Component
public class SentryTracer {

    /**
     * 값을 반환하는 구간을 span으로 감싸 계측한다. 예외는 span에 기록 후 그대로 전파한다.
     *
     * @param operation   span 구분용 짧은 이름 (예: {@code db_fetch}, {@code mapping})
     * @param description 사람이 읽을 설명 (예: {@code recordRepository.findAll})
     */
    public <T> T span(String operation, String description, Supplier<T> call) {
        ISpan parentSpan = Sentry.getSpan();
        ISpan span = parentSpan != null ? parentSpan.startChild(operation, description) : null;

        try {
            T value = call.get();
            if (span != null) {
                span.setStatus(SpanStatus.OK);
            }
            return value;
        } catch (RuntimeException e) {
            if (span != null) {
                span.setThrowable(e);
                span.setStatus(SpanStatus.INTERNAL_ERROR);
            }
            throw e;
        } finally {
            if (span != null) {
                span.finish();
            }
        }
    }
}
