package org.livef.livef_dataservice.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.livef.livef_dataservice.client.ApiFootballClient;
import org.livef.livef_dataservice.dto.TeamListResponse; // ⭐ 새로 정의한 DTO Import
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.time.Duration;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TeamCachingScheduler {

    private final ApiFootballClient apiFootballClient;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;


    // 5대 리그 ID (API-Football 기준)
    private static final List<Integer> TOP_5_LEAGUES = List.of(
            39,     // 프리미어리그
            140,    // 라리가
            61,     // 리그1
            135,    // 세리에 A
            78      // 분데스리가
    );

    @PostConstruct
    public void runInitialCache() {
        log.info("Starting initial cache run on application startup.");
        this.cacheLeagueTeams();
    }

    @Scheduled(cron = "0 0 4 * * *")
    public void cacheLeagueTeams() {
        String season = "2021";

        Flux.fromIterable(TOP_5_LEAGUES)
                .parallel(5)
                .runOn(Schedulers.boundedElastic())
                .flatMap(leagueId -> fetchAndCacheTeams(leagueId, season))
                .then()
                .subscribe();
    }

    // ⭐ DTO 타입 변경: ApiFootballResponse 대신 TeamListResponse 사용
    private Mono<Void> fetchAndCacheTeams(Integer leagueId, String season) {
        String key = "teams:league:" + leagueId;

        return apiFootballClient.fetchTeamsByLeague(leagueId, season)
                .flatMap(response -> {
                    if (response == null || response.getResponse() == null || response.getResponse().isEmpty()) {
                        // ⚠️ 빈 응답인 경우: Mono.empty() 후 성공 로그가 나올 수 있음.
                        log.warn("API returned empty response for league: {}", leagueId);
                        return Mono.empty();
                    }

                    return Mono.fromCallable(() ->
                                    objectMapper.writeValueAsString(response.getResponse())
                            )
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(json -> {
                                // ⭐ 디버그 로그 추가: 저장될 JSON 문자열 확인
                                log.info("DEBUG JSON for {}: {}", key, json.substring(0, Math.min(json.length(), 200)) + "..."); // 앞 200자만 출력

                                return redisTemplate.opsForValue()
                                        .set(key, json, Duration.ofDays(7))
                                        .then();
                            });
                })
                .doOnSuccess(v -> log.info("Cached teams for league: {}", leagueId))
                .doOnError(e -> log.error("Failed to cache teams for league: {}", leagueId, e))
                .onErrorResume(e -> Mono.empty());
    }

}