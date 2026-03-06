package org.livef.livef_dataservice.service;

import reactor.core.publisher.Mono;

public interface TeamService {
    Mono<String> getTeamsByLeague(Integer leagueId);
}
