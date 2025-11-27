package org.livef.livef_dataservice.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ReactiveRedisTemplate<String, String> redisTemplate;  // 비동기
    private final ObjectMapper objectMapper;
    private final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;

    // 💡 fixedRate를 14분(840000)에서 15초(15000)로 되돌리는 것을 고려해 보세요.
    // 실시간 데이터는 1분 이내의 주기가 일반적입니다. (기존 논의 15초)
    @Scheduled(fixedRate = 840000)
    public void pollAndCacheFixtures() {
        LocalDate today = LocalDate.now();
        List<LocalDate> dates = List.of(today.minusDays(1), today, today.plusDays(1));

        Flux.fromIterable(dates)
                .parallel(3)  // 최대 3개 병렬
                .runOn(Schedulers.boundedElastic())
                .flatMap(date -> processDate(date.format(DATE_FORMATTER)))
                .then()
                .subscribe();
    }

    private Mono<Void> processDate(String dateString) {
        String key = "fixtures:" + dateString;
        return apiFootballClient.fetchFixturesByDate(dateString)
                .flatMap(response -> {
                    if (response == null || response.getResponse().isEmpty()) {
                        return redisTemplate.opsForValue()
                                .set(key, "[]", Duration.ofMinutes(60))
                                .then();
                    }
                    return Mono.fromCallable(() ->
                                    objectMapper.writeValueAsString(response.getResponse())
                            )
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(json ->
                                    redisTemplate.opsForValue()
                                            .set(key, json, Duration.ofMinutes(60))
                                            .then()
                            );
                })
                .doOnSuccess(v -> log.info("Cached: {}", key))
                .doOnError(e -> log.error("Failed: {}", key, e))
                .onErrorResume(e ->
                        redisTemplate.opsForValue().set(key, "[]", Duration.ofMinutes(60)).then()
                );
    }
}