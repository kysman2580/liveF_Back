package org.livef.livef_dataservice.repoisitory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class FixtureCacheRepository {

    private final RedisTemplate<String, String> redisTemplate;
    // 모든 날짜 포맷에 이 변수를 사용하도록 일관성을 유지합니다.
    private final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;

    /**
     * Redis에서 오늘 날짜의 경기 목록 JSON 문자열을 조회합니다.
     * @return 저장된 JSON 문자열 (캐시 미스 시 null)
     */
    public String getTodayFixturesJson() {
        // 💡 개선: getFixturesJsonByDate 메서드를 사용하여 로직 재사용
        String todayDate = LocalDate.now().format(DATE_FORMATTER);
        return getFixturesJsonByDate(todayDate);
    }

    /**
     * 현재 날짜를 기준으로 어제, 오늘, 내일 (총 3일)의 경기 목록 JSON 문자열을 조회합니다.
     * 새벽 경기와 날짜가 넘어가는 경기를 포괄하기 위해 사용됩니다.
     * @return 3일 동안 Redis에서 조회된 JSON 문자열 리스트 (데이터가 없는 날짜는 null로 처리되어 리스트에 포함되지 않음)
     */
    public List<String> getThreeDayFixturesJson() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate tomorrow = today.plusDays(1);

        List<LocalDate> datesToFetch = List.of(yesterday, today, tomorrow);
        List<String> results = new ArrayList<>();

        log.info("🔍 Fetching 3 days of fixtures: Yesterday({}), Today({}), Tomorrow({})",
                yesterday.format(DATE_FORMATTER), today.format(DATE_FORMATTER), tomorrow.format(DATE_FORMATTER));

        for(LocalDate date : datesToFetch){
            String dateString = date.format(DATE_FORMATTER);

            // getFixturesJsonByDate(dateString) 메서드 재사용
            String json = getFixturesJsonByDate(dateString);
            if(json != null){
                results.add(json);
            }
        }
        return results;
    }

    /**
     * 특정 날짜의 경기 목록 JSON 문자열을 조회합니다.
     * 스케줄러가 저장하는 키 형식과 동일하게 사용합니다: "fixtures:YYYY-MM-DD"
     * @param date ISO 형식의 날짜 (예: "2025-10-20")
     * @return 저장된 JSON 문자열 (캐시 미스 시 null)
     */
    public String getFixturesJsonByDate(String date) {
        String redisKey = "fixtures:" + date;

        log.info("🔍 Fetching from Redis - Key: {}", redisKey);

        try {
            String json = redisTemplate.opsForValue().get(redisKey);

            if (json == null || json.trim().isEmpty()) {
                log.warn("⚠️ No data found in Redis for key: {}", redisKey);
                return null;
            }

            log.info("✅ Successfully fetched data from Redis - Key: {}, Length: {}", redisKey, json.length());
            return json;
        } catch (Exception e) {
            log.error("❌ Error fetching data from Redis - Key: {}", redisKey, e);
            return null;
        }
    }
}