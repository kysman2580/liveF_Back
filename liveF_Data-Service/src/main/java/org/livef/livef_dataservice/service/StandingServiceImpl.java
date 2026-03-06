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
public class StandingServiceImpl implements StandingService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Override
    @Cacheable(value = "standings", key = "#leagueId", cacheManager = "cacheManager")
    public Mono<String> getStandingsByLeague(Integer leagueId) {
        log.info("--- [L2 MISS] Fetching standings from Redis for league: {} ---", leagueId);
        String key = "standings:league:" + leagueId;

        return redisTemplate.opsForValue()
                .get(key)
                .defaultIfEmpty("[]")
                .cache();
    }
}
