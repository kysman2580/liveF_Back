package org.livef.livef_dataservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.livef.livef_dataservice.dto.TodayFixtureDetail;
import org.livef.livef_dataservice.service.FixtureFeedService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/feed/fixtures")
@Slf4j
public class TodayFixtureController {

    private final FixtureFeedService service;  // Reactive Service

    @GetMapping
    public Mono<ResponseEntity<List<TodayFixtureDetail>>> getTodayFixturesByLeague(
            @RequestParam int leagueId) {
        return service.getThreeDayFixturesByLeague(leagueId)
                .collectList()
                .map(list -> ResponseEntity.ok(list))
                .defaultIfEmpty(ResponseEntity.ok(Collections.emptyList()));
    }
}