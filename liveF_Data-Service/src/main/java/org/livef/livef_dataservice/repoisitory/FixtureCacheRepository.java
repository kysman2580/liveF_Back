package org.livef.livef_dataservice.repoisitory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class FixtureCacheRepository {

    private final ReactiveRedisTemplate<String, String> redisTemplate;    // 모든 날짜 포맷에 이 변수를 사용하도록 일관성을 유지합니다.
    private final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;


    /**
     * 현재 날짜를 기준으로 어제, 오늘, 내일 (총 3일)의 경기 목록 JSON 문자열을 조회합니다.
     * 새벽 경기와 날짜가 넘어가는 경기를 포괄하기 위해 사용됩니다.
     * @return 3일 동안 Redis에서 조회된 JSON 문자열 리스트 (데이터가 없는 날짜는 null로 처리되어 리스트에 포함되지 않음)
     */
    public Flux<String> getThreeDayFixturesJson() {
        LocalDate today = LocalDate.now();
        return Flux.just(today.minusDays(1), today, today.plusDays(1))
                .flatMap(date -> getFixturesJsonByDate(date.format(DATE_FORMATTER)));
    }

    /**
     * 특정 날짜의 경기 목록 JSON 문자열을 조회합니다.
     * 스케줄러가 저장하는 키 형식과 동일하게 사용합니다: "fixtures:YYYY-MM-DD"
     * @param date ISO 형식의 날짜 (예: "2025-10-20")
     * @return 저장된 JSON 문자열 (캐시 미스 시 null)
     */
    public Mono<String> getFixturesJsonByDate(String date) {
        String key = "fixtures:" + date;
        return redisTemplate.opsForValue()
                .get(key)
                .switchIfEmpty(Mono.just("[]"))
                .doOnNext(json -> log.info("Redis Hit: {}", key))
                .doOnError(e -> log.error("Redis Error: {}", key, e));
    }
}