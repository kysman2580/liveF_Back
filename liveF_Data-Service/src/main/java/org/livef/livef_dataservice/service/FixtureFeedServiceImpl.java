package org.livef.livef_dataservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.livef.livef_dataservice.dto.Score;
import org.livef.livef_dataservice.dto.Status;
import org.livef.livef_dataservice.dto.TodayFixtureDetail;
import org.livef.livef_dataservice.dto.TodayFixtureResponse;
import org.livef.livef_dataservice.repoisitory.FixtureCacheRepository;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FixtureFeedServiceImpl implements FixtureFeedService {
    private final FixtureCacheRepository fixtureCacheRepository;
    private final ObjectMapper objectMapper;

    /*@Override
    @Deprecated
    public List<TodayFixtureDetail> getTodayFixturesByLeague(int leagueId) {
        // 기존 로직 유지 (단일 날짜만 처리)
        // 이 메서드는 더 이상 사용하지 않고 getThreeDayFixturesByLeague를 사용하도록 권장합니다.
        String json = fixtureCacheRepository.getTodayFixturesJson();
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return processFixturesJson(List.of(json), leagueId);
    }*/

    // 💡 새로운 메서드: 3일치 데이터를 통합 처리
    @Override
    public List<TodayFixtureDetail> getThreeDayFixturesByLeague(int leagueId) {
        // 1. Repository에서 어제, 오늘, 내일의 JSON 리스트를 가져옵니다.
        List<String> threeDayJsons = fixtureCacheRepository.getThreeDayFixturesJson();
        log.info("Successfully fetched {} days of JSON data from cache.", threeDayJsons.size());

        if (threeDayJsons.isEmpty()) {
            log.warn("No fixture data found in cache for the 3-day range.");
            return Collections.emptyList();
        }

        // 2. JSON 리스트를 처리하여 최종 TodayFixtureDetail 리스트를 얻습니다.
        List<TodayFixtureDetail> details = processFixturesJson(threeDayJsons, leagueId);

        // 3. 3일치 데이터를 시간 순으로 통합 정렬합니다. (가장 중요)
        // 킥오프 시간(kickoffTime) 기준으로 오름차순 정렬합니다.
        details.sort((d1, d2) -> {
            if (d1.getKickoffTime() == null) return 1;
            if (d2.getKickoffTime() == null) return -1;
            return d1.getKickoffTime().compareTo(d2.getKickoffTime());
        });

        log.info("Total {} fixtures processed and sorted for league ID {}.", details.size(), leagueId);
        return details;
    }


    // 💡 JSON 리스트를 받아 처리하는 공통 로직 분리
    private List<TodayFixtureDetail> processFixturesJson(List<String> jsons, int leagueId) {
        List<TodayFixtureResponse> allFixtures = new ArrayList<>();

        for (String json : jsons) {
            if (json == null || json.trim().isEmpty()) continue;

            try {
                // 1. JSON 유효성 및 형식 검사
                JsonNode jsonNode = objectMapper.readTree(json);
                if (!jsonNode.isArray()) {
                    log.error("Invalid JSON structure: not an array. JSON snippet: {}", json.substring(0, Math.min(json.length(), 200)));
                    continue;
                }

                // 2. DTO 역직렬화 및 리스트 추가
                List<TodayFixtureResponse> dayFixtures = objectMapper.readValue(json, new TypeReference<List<TodayFixtureResponse>>() {});
                allFixtures.addAll(dayFixtures);

            } catch (JsonProcessingException e) {
                log.error("Failed to parse or deserialize JSON chunk.", e);
            } catch (Exception e) {
                log.error("Unexpected error during fixture deserialization of a chunk.", e);
            }
        }

        // 3. 인자로 받은 leaguesId로 필터링
        List<TodayFixtureResponse> filteredFixtures = allFixtures.stream()
                .filter(fixture ->
                        fixture.getLeague() != null &&
                                fixture.getLeague().getId() == leagueId)
                .toList();

        // 4. 최종 Detail DTO로 변환
        return transformToDetails(filteredFixtures);
    }


    // ******************** 기존 도우미 메서드 유지 ********************

    private List<TodayFixtureDetail> transformToDetails(List<TodayFixtureResponse> fixtures) {
        return fixtures.stream()
                .map(this::toFixtureDetail)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private TodayFixtureDetail toFixtureDetail(TodayFixtureResponse fixture) {
        try {
            log.debug("Transforming fixture ID: {}", fixture.getFixture() != null ? fixture.getFixture().getId() : "unknown");

            return TodayFixtureDetail.builder()
                    .fixtureId(fixture.getFixture() != null ? (long) fixture.getFixture().getId() : null)
                    .leagueName(fixture.getLeague() != null ? fixture.getLeague().getName() : "Unknown League")
                    .homeTeamName(fixture.getTeams() != null && fixture.getTeams().getHome() != null ?
                            fixture.getTeams().getHome().getName() : "Unknown Home")
                    .awayTeamName(fixture.getTeams() != null && fixture.getTeams().getAway() != null ?
                            fixture.getTeams().getAway().getName() : "Unknown Away")
                    .homeTeamLogoUrl(fixture.getTeams() != null && fixture.getTeams().getHome() != null ?
                            fixture.getTeams().getHome().getLogo() : null)
                    .awayTeamLogoUrl(fixture.getTeams() != null && fixture.getTeams().getAway() != null ?
                            fixture.getTeams().getAway().getLogo() : null)
                    .status(fixture.getFixture() != null && fixture.getFixture().getStatus() != null && fixture.getFixture().getStatus().getShortName() != null ?
                            fixture.getFixture().getStatus().getShortName() : "NS")
                    .score(formatScore(fixture.getScore()))
                    .kickoffTime(fixture.getFixture() != null && fixture.getFixture().getDateTime() != null ?
                            fixture.getFixture().getDateTime().atZone(ZoneId.systemDefault()).toLocalDateTime() : null)
                    .venue(fixture.getFixture() != null && fixture.getFixture().getVenue() != null ?
                            fixture.getFixture().getVenue().getName() : "Unknown Venue")
                    .time(formatTime(fixture.getFixture() != null ? fixture.getFixture().getStatus() : null))
                    .build();
        } catch (Exception e) {
            log.error("Failed to transform fixture: {}", fixture, e);
            return null;
        }
    }

    private String formatScore(Score score) {
        if (score == null || score.getFulltime() == null ||
                score.getFulltime().getHome() == null || score.getFulltime().getAway() == null) {
            return "0 - 0";
        }
        return String.format("%d - %d", score.getFulltime().getHome(), score.getFulltime().getAway());
    }

    private String formatTime(Status status) {
        if (status == null || status.getShortName() == null) {
            return "--";
        }
        if ("NS".equals(status.getShortName())) {
            return "--";
        }
        Integer elapsed = status.getElapsed();
        return elapsed != null && elapsed > 0 ? elapsed + "분" : "--";
    }
}