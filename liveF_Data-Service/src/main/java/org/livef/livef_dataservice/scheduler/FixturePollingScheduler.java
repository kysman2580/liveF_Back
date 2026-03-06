package org.livef.livef_dataservice.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.livef.livef_dataservice.client.ApiFootballClient;
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

    @PostConstruct
    public void runInitialPoll() {
        log.info("=== [FIXTURES] Scheduling initial poll (will start in 5 seconds) ===");
        Mono.delay(Duration.ofSeconds(5))
                .then(Mono.fromRunnable(this::pollAndCacheFixtures))
                .subscribe();
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void pollAndCacheFixtures() {
        LocalDate today = LocalDate.now();
        List<LocalDate> dates = List.of(today.minusDays(1), today, today.plusDays(1));
        log.info("=== [FIXTURES] Polling started for dates: {} ===", dates);

        Flux.fromIterable(dates)
                .concatMap(date -> fetchAndCacheForDate(date)
                        .delayElement(Duration.ofSeconds(2)))
                .then()
                .doOnSuccess(v -> log.info("=== [FIXTURES] Polling completed ==="))
                .subscribe();
    }

    private Mono<Void> fetchAndCacheForDate(LocalDate date) {
        String dateStr = date.format(DATE_FORMATTER);
        String key = "fixtures:" + dateStr;

        return redisTemplate.hasKey(key)
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        log.info("[FIXTURES] 🟢 Already exists for date: {}. Skipping API.", dateStr);
                        return Mono.empty();
                    }

                    log.info("[FIXTURES] 🔵 Fetching date: {} ...", dateStr);
                    return apiFootballClient.fetchFixturesByDate(dateStr)
                            .flatMap(response -> {
                                if (response == null || response.getResponse() == null) {
                                    log.error("[FIXTURES] ❌ Response NULL for date: {}", dateStr);
                                    return saveEmpty(key);
                                }
                                if (response.getResponse().isEmpty()) {
                                    log.warn("[FIXTURES] ⚠️ No fixtures for date: {}", dateStr);
                                    return saveEmpty(key);
                                }
                                return Mono.fromCallable(() -> objectMapper.writeValueAsString(response.getResponse()))
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .flatMap(json -> redisTemplate.opsForValue()
                                                .set(key, json, Duration.ofDays(7))
                                                .then());
                            });
                })
                .doOnSuccess(v -> log.info("[FIXTURES] ✅ Done: date:{}", dateStr))
                .doOnError(e -> log.error("[FIXTURES] ❌ Error: date:{} - {}", dateStr, e.getMessage()))
                .then() // Ensure return type is Mono<Void>
                .onErrorResume(e -> Mono.empty());
    }

    private Mono<Void> saveEmpty(String key) {
        return redisTemplate.opsForValue()
                .set(key, "[]", Duration.ofDays(7))
                .then();
    }
}
