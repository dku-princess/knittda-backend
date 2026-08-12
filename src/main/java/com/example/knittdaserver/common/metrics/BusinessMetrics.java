package com.example.knittdaserver.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * 비즈니스/제품 이벤트 발생량을 계측하는 공통 헬퍼 (M4 — Business Ops Metrics).
 *
 * <p>시스템 건강(M1~M3)을 넘어 "가입·작품 생성·검색 같은 제품 이벤트가 실제로
 * 발생·성공하는가"를 운영 관점에서 실시간 계측한다. 모든 지표는 Counter 이며
 * Prometheus 에서 {@code rate()} 로 추세를, {@code result} 라벨로 성공률을 본다.
 *
 * <h3>네이밍 규칙</h3>
 * {@code business.<domain>.<event>} — 예) {@code business.project.created},
 * {@code business.auth.login}. Prometheus 노출 시 {@code business_<domain>_<event>_total}.
 *
 * <h3>카디널리티 원칙 (필수)</h3>
 * 태그에는 {@code result / provider / version} 같은 <b>저카디널리티 값만</b> 넣는다.
 * {@code userId / projectId} 등 고유값은 시계열을 폭발시키므로 절대 금지 —
 * per-user 퍼널·리텐션·코호트 분석은 Prometheus 밖(BigQuery/Amplitude)에서 한다.
 *
 * <h3>계측 지점</h3>
 * 도메인 로직이 실제 성공한 지점(서비스 계층)에서 증가시켜, 실패/롤백을
 * 성공으로 오계측하지 않도록 한다.
 */
@Component
public class BusinessMetrics {

    private static final String PREFIX = "business.";

    private final MeterRegistry registry;

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * 태그 없는 비즈니스 이벤트 1건을 기록한다.
     *
     * @param event {@code <domain>.<event>} 형태 (예: {@code report.created})
     */
    public void count(String event) {
        Counter.builder(PREFIX + event)
                .description("비즈니스 이벤트 발생 수")
                .register(registry)
                .increment();
    }

    /**
     * 저카디널리티 태그 1쌍을 붙여 비즈니스 이벤트 1건을 기록한다.
     *
     * @param event    {@code <domain>.<event>} 형태 (예: {@code auth.login})
     * @param tagKey   태그 키 (예: {@code provider}, {@code result}) — 저카디널리티만
     * @param tagValue 태그 값 (예: {@code kakao}, {@code success}) — 고유값 금지
     */
    public void count(String event, String tagKey, String tagValue) {
        Counter.builder(PREFIX + event)
                .description("비즈니스 이벤트 발생 수")
                .tag(tagKey, tagValue)
                .register(registry)
                .increment();
    }

    /**
     * 저카디널리티 태그 2쌍을 붙여 비즈니스 이벤트 1건을 기록한다.
     */
    public void count(String event, String tagKey1, String tagValue1, String tagKey2, String tagValue2) {
        Counter.builder(PREFIX + event)
                .description("비즈니스 이벤트 발생 수")
                .tag(tagKey1, tagValue1)
                .tag(tagKey2, tagValue2)
                .register(registry)
                .increment();
    }
}
