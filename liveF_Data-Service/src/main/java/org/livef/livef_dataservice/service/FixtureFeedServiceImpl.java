package org.livef.livef_dataservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.livef.livef_dataservice.dto.Score;
import org.livef.livef_dataservice.dto.Status;
import org.livef.livef_dataservice.dto.TodayFixtureDetail;
import org.livef.livef_dataservice.dto.TodayFixtureResponse;
import org.livef.livef_dataservice.repoisitory.FixtureCacheRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FixtureFeedServiceImpl implements FixtureFeedService {

    private final FixtureCacheRepository fixtureCacheRepository;
    private final ObjectMapper objectMapper;


    // 💡 새로운 메서드: 3일치 데이터를 통합 처리
    @Override
    public Flux<TodayFixtureDetail> getThreeDayFixturesByLeague(int leagueId) {
        return fixtureCacheRepository.getThreeDayFixturesJson()  // Flux<String>
                .flatMap(json -> parseJsonAsync(json))  // Flux<TodayFixtureResponse>
                .filter(f -> f.getLeague() != null && f.getLeague().getId() == leagueId)
                .map(this::toFixtureDetail)
                .filter(Objects::nonNull)
                .sort(Comparator.comparing(TodayFixtureDetail::getKickoffTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .doOnNext(detail -> log.debug("Emitting fixture: {}", detail.getFixtureId()))
                .doOnError(e -> log.error("Service error for leagueId: {}", leagueId, e))
                .onErrorResume(e -> Flux.empty());
    }


    // JSON 파싱을 비동기로 처리
    private Flux<TodayFixtureResponse> parseJsonAsync(String json) {
        if (json == null || json.trim().isEmpty() || "[]".equals(json)) {
            return Flux.empty();
        }

        return Mono.fromCallable(() ->
                        objectMapper.readValue(json, new TypeReference<List<TodayFixtureResponse>>() {})
                )
                .subscribeOn(Schedulers.boundedElastic())  // CPU 작업 분리
                .flatMapMany(Flux::fromIterable)
                .onErrorResume(e -> {
                    log.error("JSON parsing failed: {}", json.substring(0, Math.min(200, json.length())), e);
                    return Flux.empty();
                });
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