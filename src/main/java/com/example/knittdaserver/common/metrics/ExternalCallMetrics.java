package com.example.knittdaserver.common.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 외부 의존성(S3, OpenAI, Flask 등) 호출을 계측하는 공통 헬퍼.
 *
 * <p>메트릭 {@code external.call} (Timer)을 다음 태그로 기록한다.
 * <ul>
 *   <li>{@code target}    — 외부 대상 (s3 / openai / flask)</li>
 *   <li>{@code operation} — 호출 종류 (upload / delete / embedding / search / index_delete)</li>
 *   <li>{@code result}    — success / error (실패율 산출용)</li>
 * </ul>
 *
 * <p>Prometheus 에서는 {@code external_call_seconds_*} 로 노출되며,
 * 지연 분포(p95/p99)와 {@code result="error"} 비율(실패율)을 함께 볼 수 있다.
 */
@Component
public class ExternalCallMetrics {

    private static final String METRIC = "external.call";

    private final MeterRegistry registry;

    public ExternalCallMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * 값을 반환하는 외부 호출을 계측한다. 예외는 result=error 로 기록 후 그대로 전파한다.
     */
    public <T> T record(String target, String operation, Supplier<T> call) {
        Timer.Sample sample = Timer.start(registry);
        String result = "success";
        try {
            return call.get();
        } catch (RuntimeException e) {
            result = "error";
            throw e;
        } finally {
            sample.stop(Timer.builder(METRIC)
                    .description("외부 의존성 호출 소요 시간 및 결과")
                    .tag("target", target)
                    .tag("operation", operation)
                    .tag("result", result)
                    .register(registry));
        }
    }

    /**
     * 반환값이 없는 외부 호출을 계측한다.
     */
    public void record(String target, String operation, Runnable call) {
        record(target, operation, () -> {
            call.run();
            return null;
        });
    }
}
