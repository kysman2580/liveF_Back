package org.livef.livef_dataservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.livef.livef_dataservice.service.StandingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/feed/standings")
public class StandingController {

    private final StandingService standingService;

    @GetMapping
    public Mono<String> getStandingsByLeague(@RequestParam("leagueId") Integer leagueId) {
        log.info("=== Standing API Called: /api/v1/feed/standings?leagueId={} ===", leagueId);
        return standingService.getStandingsByLeague(leagueId);
    }
}
