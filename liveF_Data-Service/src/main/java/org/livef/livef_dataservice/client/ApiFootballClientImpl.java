package org.livef.livef_dataservice.client;

import lombok.RequiredArgsConstructor;
import org.livef.livef_dataservice.dto.ApiFootballResponse;
import org.livef.livef_dataservice.dto.TeamListResponse; // ⭐ Team DTO
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RequiredArgsConstructor
@Component
public class ApiFootballClientImpl implements ApiFootballClient {

    private final WebClient webClient;

    @Override
    public Mono<ApiFootballResponse> fetchFixturesByDate(String date) {

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/fixtures")
                        .queryParam("date", date)
                        .build())
                .retrieve()
                .bodyToMono(ApiFootballResponse.class)
                .onErrorResume(e -> {
                    System.err.println("Error fetching fixtures: " + e.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * 특정 리그의 전체 팀 목록 조회
     */
    // ⭐ 반환 타입을 TeamListResponse로 변경
    public Mono<TeamListResponse> fetchTeamsByLeague(Integer leagueId, String season) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/teams")
                        .queryParam("league", leagueId)
                        .queryParam("season", season)
                        .build())
                .retrieve()
                // ⭐ DTO 타입 변경
                .bodyToMono(TeamListResponse.class)
                .timeout(Duration.ofSeconds(10))
                .retry(2);
    }
}

