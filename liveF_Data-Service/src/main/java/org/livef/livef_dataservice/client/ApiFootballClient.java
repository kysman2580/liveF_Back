package org.livef.livef_dataservice.client;

import org.livef.livef_dataservice.dto.ApiFootballResponse;
import org.livef.livef_dataservice.dto.TeamListResponse;
import reactor.core.publisher.Mono;

public interface ApiFootballClient {

    Mono<ApiFootballResponse> fetchFixturesByDate(String date);

    Mono<TeamListResponse> fetchTeamsByLeague(Integer leagueId, String season);
}
