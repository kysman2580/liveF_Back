package org.livef.livef_dataservice.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.livef.livef_dataservice.client.ApiFootballClient;
import org.livef.livef_dataservice.dto.ApiFootballResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class FixturePollingScheduler {

    private final ApiFootballClient apiFootballClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;

    // 💡 fixedRate를 14분(840000)에서 15초(15000)로 되돌리는 것을 고려해 보세요.
    // 실시간 데이터는 1분 이내의 주기가 일반적입니다. (기존 논의 15초)
    @Scheduled(fixedRate = 840000) // 현재 14분 유지
    public void pollAndCacheFixtures() {
        LocalDate today = LocalDate.now();

        // 어제, 오늘, 내일의 날짜를 리스트로 계산합니다.
        List<LocalDate> datesToPoll = List.of(
                today.minusDays(1), // 어제
                today,              // 오늘
                today.plusDays(1)   // 내일
        );

        // 각 날짜에 대해 API 호출 및 캐싱 로직을 수행합니다.
        for (LocalDate date : datesToPoll) {
            String dateString = date.format(DATE_FORMATTER);
            processDate(dateString);
        }
    }

    private void processDate(String dateString) {
        String redisKey = "fixtures:" + dateString;

        log.info("Polling API-Football for date: {}", dateString);

        // API 클라이언트 호출 (Mono를 반환하므로 체이닝을 사용)
        apiFootballClient.fetchFixturesByDate(dateString)
                .flatMap(response -> {
                    try {
                        String jsonResponse;
                        int fixtureCount = 0;

                        if (response != null && response.getResponse() != null && !response.getResponse().isEmpty()) {
                            jsonResponse = convertResponseToJson(response);
                            fixtureCount = response.getResponse().size();
                            log.info("Successfully fetched and serialized {} fixtures for {}.", fixtureCount, dateString);
                        } else {
                            jsonResponse = "[]";
                            log.warn("Empty or null response from API for date: {}", dateString);
                        }

                        // Redis에 저장 (60분 TTL 유지)
                        redisTemplate.opsForValue().set(redisKey, jsonResponse, 60, TimeUnit.MINUTES);
                        log.info("Successfully cached data. Key: {}", redisKey);

                        return Mono.empty(); // 성공적으로 처리했음을 알림

                    } catch (JsonProcessingException e) {
                        log.error("Error serializing API response to JSON for date: {}", dateString, e);
                        return Mono.error(e); // 직렬화 오류 시 에러 발생
                    }
                })
                .doOnError(e -> log.error("Failed to fetch or process API response for date: {}", dateString, e))
                .subscribe(); // Mono 실행
    }


    private String convertResponseToJson(ApiFootballResponse response) throws JsonProcessingException {
        // response.getResponse()는 List<FixtureResponse>로 예상되므로, 이를 JSON Array로 변환합니다.
        return objectMapper.writeValueAsString(response.getResponse());
    }
}