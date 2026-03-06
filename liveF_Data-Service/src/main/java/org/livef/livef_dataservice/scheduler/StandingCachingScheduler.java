package org.livef.livef_dataservice.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.livef.livef_dataservice.client.ApiFootballClient;
import org.livef.livef_dataservice.dto.LeagueStanding;
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
public class StandingCachingScheduler {

    private final ApiFootballClient apiFootballClient;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final List<Integer> TOP_5_LEAGUES = List.of(
            39, // 프리미어리그
            140, // 라리가
            61, // 리그1
            135, // 세리에 A
            78, // 분데스리가
            2 // UEFA Champions League
    );

    private static final String FREE_PLAN_SEASON = "2024";
    private static final int DELAY_SECONDS = 7;

    @PostConstruct
    public void runInitialCache() {
        log.info("=== [STANDINGS] Scheduling initial cache (will start in 45 seconds) ===");
        Mono.delay(Duration.ofSeconds(45))
                .then(Mono.defer(this::cacheLeagueStandingsAsync))
                .subscribe(
                        v -> log.info("=== [STANDINGS] Initial cache sequence completed ==="),
                        e -> log.error("=== [STANDINGS] Initial cache sequence failed ===", e));
    }

    @Scheduled(cron = "0 30 4 * * *") // TeamCachingScheduler runs at 4:00, this runs at 4:30
    public void cacheLeagueStandings() {
        cacheLeagueStandingsAsync().subscribe();
    }

    private Mono<Void> cacheLeagueStandingsAsync() {
        log.info("=== [STANDINGS] Caching started (Season: {}) ===", FREE_PLAN_SEASON);
        return Flux.fromIterable(TOP_5_LEAGUES)
                .index()
                .concatMap(tuple -> {
                    long index = tuple.getT1();
                    Integer leagueId = tuple.getT2();
                    Duration delay = index == 0 ? Duration.ZERO : Duration.ofSeconds(DELAY_SECONDS);
                    return Mono.delay(delay).then(fetchWithRetry(leagueId, FREE_PLAN_SEASON));
                })
                .then();
    }

    private Mono<Void> fetchWithRetry(Integer leagueId, String season) {
        String key = "standings:league:" + leagueId;

        log.info("[STANDINGS] 🔵 Fetching league: {}, season: {} ...", leagueId, season);
        return apiFootballClient.fetchStandingsByLeague(leagueId, season)
                .flatMap(response -> {
                    if (response == null || response.hasErrors()) {
                        log.error("[STANDINGS] 🔴 API Errors or Null Response for league: {}", leagueId);
                        return Mono.empty();
                    }
                    List<LeagueStanding> standings = response.getResponse();
                    if (standings == null || standings.isEmpty()) {
                        log.warn("[STANDINGS] ⚠️ No standings for league: {}", leagueId);
                        return Mono.empty();
                    }
                    return Mono.fromCallable(() -> objectMapper.writeValueAsString(standings))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(json -> redisTemplate.opsForValue()
                                    .set(key, json, Duration.ofDays(1)) // Standings might change daily
                                    .then());
                })
                .doOnSuccess(v -> log.info("[STANDINGS] ✅ Done: league:{}", leagueId))
                .doOnError(e -> log.error("[STANDINGS] ❌ Error: league:{} - {}", leagueId, e.getMessage()))
                .then()
                .onErrorResume(e -> Mono.empty());
    }
}
