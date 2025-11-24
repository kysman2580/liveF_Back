package org.livef.livef_dataservice.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.livef.livef_dataservice.client.ApiFootballClient;
import org.livef.livef_dataservice.dto.ApiFootballResponse;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FixturePollingScheduler {

    private final ApiFootballClient apiFootballClient;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;

    @Scheduled(fixedRate = 840000)
    public void pollAndCacheFixtures() {
        LocalDate today = LocalDate.now();
        List<LocalDate> dates = List.of(
                today.minusDays(1),
                today,
                today.plusDays(1)
        );

        log.info("=== [FIXTURES] Polling started for dates: {} ===", dates);

        Flux.fromIterable(dates)
                .concatMap(date ->
                        processDate(date.format(DATE_FORMATTER))
                                .delayElement(Duration.ofSeconds(2))
                )
                .then()
                .doOnSuccess(v -> log.info("=== [FIXTURES] Polling completed ==="))
                .subscribe();
    }

    private Mono<Void> processDate(String dateString) {
        String key = "fixtures:" + dateString;

        log.info("========================================");
        log.info("[FIXTURES] 🔵 START: Processing date: {}", dateString);
        log.info("========================================");

        return apiFootballClient.fetchFixturesByDate(dateString)
                .doOnNext(response -> {
                    log.info("[FIXTURES] 📦 Response received for date: {}", dateString);
                    log.info("[FIXTURES] 📦 Response object: {}", response != null ? "NOT NULL" : "NULL");

                    if (response != null) {
                        log.info("[FIXTURES] 📦 Response.get: {}", response.getGet());
                        log.info("[FIXTURES] 📦 Response.results: {}", response.getResults());
                        log.info("[FIXTURES] 📦 Response.errors: {}", response.getErrors());
                        log.info("[FIXTURES] 📦 Response.response: {}",
                                response.getResponse() != null ? "NOT NULL" : "NULL");

                        if (response.getResponse() != null) {
                            log.info("[FIXTURES] 📦 Response.response.size(): {}",
                                    response.getResponse().size());
                        }
                    }
                })
                .flatMap(response -> {
                    // 1단계: response null 체크
                    if (response == null) {
                        log.error("[FIXTURES] ❌ STEP 1 FAILED: Response is NULL");
                        return saveEmptyAndLog(key, "Response is NULL");
                    }
                    log.info("[FIXTURES] ✅ STEP 1 PASSED: Response is not null");

                    // 2단계: response.getResponse() null 체크
                    if (response.getResponse() == null) {
                        log.error("[FIXTURES] ❌ STEP 2 FAILED: Response.response is NULL");
                        return saveEmptyAndLog(key, "Response.response is NULL");
                    }
                    log.info("[FIXTURES] ✅ STEP 2 PASSED: Response.response is not null");

                    // 3단계: 빈 배열 체크
                    if (response.getResponse().isEmpty()) {
                        log.warn("[FIXTURES] ⚠️ STEP 3: Response.response is EMPTY (no fixtures)");
                        return saveEmptyAndLog(key, "No fixtures for this date");
                    }
                    log.info("[FIXTURES] ✅ STEP 3 PASSED: Response has {} fixtures",
                            response.getResponse().size());

                    // 4단계: JSON 직렬화
                    log.info("[FIXTURES] 🔄 STEP 4: Starting JSON serialization...");
                    return Mono.fromCallable(() -> {
                                try {
                                    String json = objectMapper.writeValueAsString(response.getResponse());
                                    log.info("[FIXTURES] ✅ STEP 4 PASSED: JSON serialized ({} bytes)",
                                            json.length());
                                    log.debug("[FIXTURES] JSON preview: {}",
                                            json.substring(0, Math.min(200, json.length())) + "...");
                                    return json;
                                } catch (JsonProcessingException e) {
                                    log.error("[FIXTURES] ❌ STEP 4 FAILED: JSON serialization error", e);
                                    throw new RuntimeException("JSON serialization failed", e);
                                }
                            })
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(json -> {
                                // 5단계: Redis 저장
                                log.info("[FIXTURES] 💾 STEP 5: Saving to Redis key: {}", key);
                                return redisTemplate.opsForValue()
                                        .set(key, json, Duration.ofMinutes(60))
                                        .doOnSuccess(result -> {
                                            log.info("[FIXTURES] ✅ STEP 5 PASSED: Redis save result: {}", result);
                                        })
                                        .doOnError(e -> {
                                            log.error("[FIXTURES] ❌ STEP 5 FAILED: Redis save error", e);
                                        })
                                        .then();
                            });
                })
                .doOnSuccess(v -> {
                    log.info("========================================");
                    log.info("[FIXTURES] ✅ SUCCESS: Completed for date: {}", dateString);
                    log.info("========================================");
                })
                .doOnError(e -> {
                    log.error("========================================");
                    log.error("[FIXTURES] ❌ ERROR: Failed for date: {}", dateString);
                    log.error("[FIXTURES] Error type: {}", e.getClass().getName());
                    log.error("[FIXTURES] Error message: {}", e.getMessage());
                    log.error("[FIXTURES] Stack trace:", e);
                    log.error("========================================");
                })
                .onErrorResume(e -> {
                    log.error("[FIXTURES] ⚠️ FALLBACK: Saving empty array due to error");
                    return saveEmptyAndLog(key, "Error occurred: " + e.getMessage());
                });
    }

    private Mono<Void> saveEmptyAndLog(String key, String reason) {
        log.warn("[FIXTURES] 💾 Saving empty array to key: {} (Reason: {})", key, reason);
        return redisTemplate.opsForValue()
                .set(key, "[]", Duration.ofMinutes(60))
                .doOnSuccess(result -> log.info("[FIXTURES] Empty array saved, result: {}", result))
                .then();
    }
}