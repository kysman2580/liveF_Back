package org.livef.livef_dataservice.scheduler;

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
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TeamCachingScheduler {

    private final ApiFootballClient apiFootballClient;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final List<Integer> TOP_5_LEAGUES = List.of(
            39,     // 프리미어리그
            140,    // 라리가
            61,     // 리그1
            135,    // 세리에 A
            78      // 분데스리가
    );

    private static final String FREE_PLAN_SEASON = "2021";

    // ⭐ 무료 플랜: 분당 10 requests 제한
    // 각 요청 사이 7초 대기 (60초 / 10 = 6초, 안전하게 7초)
    private static final int DELAY_SECONDS = 7;

    @PostConstruct
    public void runInitialCache() {
        log.info("=== [TEAMS] Scheduling initial cache (will start in 30 seconds) ===");

        // ⭐⭐⭐ 중요: 앱 시작 후 30초 대기 (Fixture 캐싱과 시간차)
        Mono.delay(Duration.ofSeconds(30))
                .then(Mono.defer(this::cacheLeagueTeamsAsync))
                .subscribe(
                        v -> log.info("=== [TEAMS] Initial cache completed ==="),
                        e -> log.error("=== [TEAMS] Initial cache failed ===", e)
                );
    }

    @Scheduled(cron = "0 0 4 * * *")  // 매일 새벽 4시
    public void cacheLeagueTeams() {
        cacheLeagueTeamsAsync().subscribe();
    }

    private Mono<Void> cacheLeagueTeamsAsync() {
        log.info("=== [TEAMS] Caching started (season: {}) ===", FREE_PLAN_SEASON);
        log.info("=== [TEAMS] Rate Limit Protection: {} seconds between requests ===", DELAY_SECONDS);

        // ⭐⭐⭐ 중요: parallel 제거! 순차 실행 + 요청 사이 딜레이
        return Flux.fromIterable(TOP_5_LEAGUES)
                .index()  // (0, 39), (1, 140), (2, 61), ...
                .concatMap(tuple -> {
                    long index = tuple.getT1();
                    Integer leagueId = tuple.getT2();

                    // 첫 번째 요청은 즉시, 이후 요청은 7초씩 대기
                    Duration delay = index == 0
                            ? Duration.ZERO
                            : Duration.ofSeconds(DELAY_SECONDS);

                    return Mono.delay(delay)
                            .then(fetchAndCacheTeams(leagueId, FREE_PLAN_SEASON));
                })
                .then();
    }

    private Mono<Void> fetchAndCacheTeams(Integer leagueId, String season) {
        String key = "teams:league:" + leagueId;

        log.info("[TEAMS] 🔵 Fetching league: {}, season: {} (waiting for API...)", leagueId, season);

        return apiFootballClient.fetchTeamsByLeague(leagueId, season)
                .flatMap(response -> {
                    // 1. Response null 체크
                    if (response == null) {
                        log.error("[TEAMS] ❌ Response is NULL for league: {}", leagueId);
                        return Mono.empty();
                    }

                    // 2. Response.response null 체크
                    if (response.getResponse() == null) {
                        log.error("[TEAMS] ❌ Response.response is NULL for league: {}", leagueId);
                        return Mono.empty();
                    }

                    // 3. 빈 배열 체크
                    if (response.getResponse().isEmpty()) {
                        log.warn("[TEAMS] ⚠️ No teams for league: {} (season: {})", leagueId, season);
                        return Mono.empty();
                    }

                    // 4. 정상 데이터 처리
                    int teamCount = response.getResponse().size();
                    log.info("[TEAMS] ✅ Found {} teams for league: {}", teamCount, leagueId);

                    return Mono.fromCallable(() ->
                                    objectMapper.writeValueAsString(response.getResponse())
                            )
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(json -> {
                                log.info("[TEAMS] 💾 Saving {} bytes to Redis: {}", json.length(), key);
                                return redisTemplate.opsForValue()
                                        .set(key, json, Duration.ofDays(7))
                                        .then();
                            });
                })
                .doOnSuccess(v -> log.info("[TEAMS] ✅ Completed: league:{}", leagueId))
                .doOnError(e -> log.error("[TEAMS] ❌ Failed: league:{} - {}", leagueId, e.getMessage(), e))
                .onErrorResume(e -> {
                    log.warn("[TEAMS] ⚠️ Skipping league {} due to error", leagueId);
                    return Mono.empty();
                });
    }
}