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
        log.info("=== [TEAMS] Scheduling initial cache (will start in 30 seconds) ===");
        Mono.delay(Duration.ofSeconds(30))
                .then(Mono.defer(this::cacheLeagueTeamsAsync))
                .subscribe(
                        v -> log.info("=== [TEAMS] Initial cache sequence completed ==="),
                        e -> log.error("=== [TEAMS] Initial cache sequence failed ===", e));
    }

    @Scheduled(cron = "0 0 4 * * *")
    public void cacheLeagueTeams() {
        cacheLeagueTeamsAsync().subscribe();
    }

    private Mono<Void> cacheLeagueTeamsAsync() {
        log.info("=== [TEAMS] Caching started (Season: {}) ===", FREE_PLAN_SEASON);
        return Flux.fromIterable(TOP_5_LEAGUES)
                .index()
                .concatMap(tuple -> {
                    long index = tuple.getT1();
                    Integer leagueId = tuple.getT2();
                    Duration delay = index == 0 ? Duration.ZERO : Duration.ofSeconds(DELAY_SECONDS);
                    return Mono.delay(delay).then(fetchWithRetry(leagueId, FREE_PLAN_SEASON, 0));
                })
                .then();
    }

    private Mono<Void> fetchWithRetry(Integer leagueId, String season, int retryCount) {
        String key = "teams:league:" + leagueId;

        return redisTemplate.hasKey(key)
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        log.info("[TEAMS] 🟢 Data already exists for league: {}. Skipping API.", leagueId);
                        return Mono.empty();
                    }

                    log.info("[TEAMS] 🔵 Fetching league: {}, season: {} ...", leagueId, season);
                    return apiFootballClient.fetchTeamsByLeague(leagueId, season)
                            .flatMap(response -> {
                                if (response.hasErrors()) {
                                    log.error("[TEAMS] 🔴 API Errors: {}", response.getErrors());
                                    return Mono.empty();
                                }
                                List<org.livef.livef_dataservice.dto.TeamApiInfo> teams = response.getResponse();
                                if (teams == null || teams.isEmpty()) {
                                    log.warn("[TEAMS] ⚠️ No teams for league: {}", leagueId);
                                    return Mono.empty();
                                }
                                return Mono.fromCallable(() -> objectMapper.writeValueAsString(teams))
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .flatMap(json -> redisTemplate.opsForValue()
                                                .set(key, json, Duration.ofDays(7))
                                                .then());
                            });
                })
                .doOnSuccess(v -> log.info("[TEAMS] ✅ Done: league:{}", leagueId))
                .doOnError(e -> log.error("[TEAMS] ❌ Error: league:{} - {}", leagueId, e.getMessage()))
                .then() // Ensure return type is Mono<Void>
                .onErrorResume(e -> Mono.empty());
    }
}
