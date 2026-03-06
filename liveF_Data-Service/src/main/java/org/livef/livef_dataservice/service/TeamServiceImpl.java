package org.livef.livef_dataservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamServiceImpl implements TeamService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    /**
     * 특정 리그의 팀 목록 조회 (L1 Caffeine + L2 Redis 적용)
     * 
     * @param leagueId 리그 ID
     * @return JSON 문자열 (팀 목록)
     */
    @Override
    @Cacheable(value = "teams", key = "#leagueId")
    public Mono<String> getTeamsByLeague(Integer leagueId) {
        log.info("--- [L2 MISS] Fetching teams from Redis for league: {} ---", leagueId);
        String key = "teams:league:" + leagueId;

        return redisTemplate.opsForValue()
                .get(key)
                .defaultIfEmpty("[]")
                .cache(); // Mono 캐싱을 통해 구독 시점의 성능 최적화
    }
}
