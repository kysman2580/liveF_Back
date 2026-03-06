package org.livef.livef_dataservice.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.livef.livef_dataservice.dto.Score;
import org.livef.livef_dataservice.dto.Status;
import org.livef.livef_dataservice.dto.TodayFixtureDetail;
import org.livef.livef_dataservice.dto.TodayFixtureResponse;
import org.livef.livef_dataservice.repository.FixtureCacheRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class FixtureFeedServiceImpl implements FixtureFeedService {

    private final FixtureCacheRepository fixtureCacheRepository;
    private final ObjectMapper objectMapper;

    /**
     * 특정 리그의 3일치 경기 목록을 효율적으로 조회합니다.
     * JSON 파싱과 필터링을 스트리밍 방식으로 처리하여 메모리 사용량을 최적화합니다.
     * 
     * @param leagueId 조회할 리그의 ID
     * @return 경기 상세 정보 Flux
     */
    @Override
    @Cacheable(value = "fixtures", key = "#leagueId")
    public Flux<TodayFixtureDetail> getThreeDayFixturesByLeague(int leagueId) {
        log.info("--- [L2 MISS] Parsing fixtures from Redis for league: {} ---", leagueId);
        return fixtureCacheRepository.getThreeDayFixturesJson() // 3일치 JSON 데이터를 담은 Flux<String>
                .flatMap(json -> parseAndFilterJsonAsync(json, leagueId)) // 스트리밍 파싱 및 필터링
                .map(this::toFixtureDetail)
                .filter(Objects::nonNull) // 변환 실패(null)된 객체 필터링
                .sort(Comparator.comparing(TodayFixtureDetail::getKickoffTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .doOnNext(detail -> log.debug("Emitting fixture: {}", detail.getFixtureId()))
                .doOnError(e -> log.error("Service error for leagueId: {}", leagueId, e))
                .onErrorResume(e -> Flux.empty())
                .cache(); // 결과를 캐싱하여 L1(Caffeine)에 Mono/Flux 형태로 저장
    }

    /**
     * JSON 문자열을 스트리밍 방식으로 파싱하고, 주어진 leagueId와 일치하는 경기만 필터링합니다.
     * 대용량 JSON 데이터를 효율적으로 처리하기 위해 스트리밍 파서를 사용합니다.
     * 
     * @param json     파싱할 JSON 문자열
     * @param leagueId 필터링할 리그 ID
     * @return 필터링된 TodayFixtureResponse 객체의 Flux
     */
    private Flux<TodayFixtureResponse> parseAndFilterJsonAsync(String json, int leagueId) {
        if (json == null || json.trim().isEmpty() || "[]".equals(json.trim())) {
            return Flux.empty();
        }

        return Flux.<TodayFixtureResponse>create(sink -> {
            try (JsonParser parser = objectMapper.getFactory().createParser(json)) {
                if (parser.nextToken() == JsonToken.START_ARRAY) {
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        if (parser.currentToken() == JsonToken.START_OBJECT) {
                            TodayFixtureResponse response = objectMapper.readValue(parser, TodayFixtureResponse.class);
                            if (response.getLeague() != null && response.getLeague().getId() == leagueId) {
                                sink.next(response);
                            }
                        }
                    }
                }
                sink.complete();
            } catch (IOException e) {
                log.error("JSON parsing failed during streaming: {}", e.getMessage());
                sink.error(e);
            }
        })
                .subscribeOn(Schedulers.boundedElastic()) // 파싱 작업을 별도 스레드에서 실행
                .onErrorResume(e -> {
                    log.error("Error in parsing stream, returning empty. Error: {}", e.getMessage());
                    return Flux.empty();
                });
    }

    private TodayFixtureDetail toFixtureDetail(TodayFixtureResponse fixture) {
        try {
            log.debug("Transforming fixture ID: {}",
                    fixture.getFixture() != null ? fixture.getFixture().getId() : "unknown");

            String statusShortName = fixture.getFixture() != null && fixture.getFixture().getStatus() != null
                    && fixture.getFixture().getStatus().getShortName() != null
                            ? fixture.getFixture().getStatus().getShortName()
                            : "NS";

            return TodayFixtureDetail.builder()
                    .fixtureId(fixture.getFixture() != null ? (long) fixture.getFixture().getId() : null)
                    .leagueName(fixture.getLeague() != null ? fixture.getLeague().getName() : "Unknown League")
                    .homeTeamName(fixture.getTeams() != null && fixture.getTeams().getHome() != null
                            ? fixture.getTeams().getHome().getName()
                            : "Unknown Home")
                    .awayTeamName(fixture.getTeams() != null && fixture.getTeams().getAway() != null
                            ? fixture.getTeams().getAway().getName()
                            : "Unknown Away")
                    .homeTeamLogoUrl(fixture.getTeams() != null && fixture.getTeams().getHome() != null
                            ? fixture.getTeams().getHome().getLogo()
                            : null)
                    .awayTeamLogoUrl(fixture.getTeams() != null && fixture.getTeams().getAway() != null
                            ? fixture.getTeams().getAway().getLogo()
                            : null)
                    .status(statusShortName)
                    .score(formatScore(fixture.getScore(), statusShortName))
                    .kickoffTime(fixture.getFixture() != null && fixture.getFixture().getDateTime() != null
                            ? fixture.getFixture().getDateTime().atZone(ZoneId.systemDefault()).toLocalDateTime()
                            : null)
                    .venue(fixture.getFixture() != null && fixture.getFixture().getVenue() != null
                            ? fixture.getFixture().getVenue().getName()
                            : "Unknown Venue")
                    .time(formatTime(fixture.getFixture() != null ? fixture.getFixture().getStatus() : null))
                    .build();
        } catch (Exception e) {
            log.error("Failed to transform fixture: {}", fixture, e);
            return null;
        }
    }

    private String formatScore(Score score, String status) {
        if ("NS".equals(status)) {
            return "VS";
        }
        if (score == null || score.getFulltime() == null ||
                score.getFulltime().getHome() == null || score.getFulltime().getAway() == null) {
            return "진행중";
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