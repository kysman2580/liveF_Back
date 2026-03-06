package org.livef.livef_dataservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.livef.livef_dataservice.service.TeamService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@Slf4j
// ⭐️ [수정] RequestMapping을 FixtureController와 유사하게 변경하거나,
// 명확하게 하나의 경로로 정의합니다. (여기서는 /api/v1/feed를 사용한다고 가정)
@RequestMapping("/api/v1/feed/teams")
public class TeamController {

    private final TeamService teamService;

    /**
     * 특정 리그의 팀 목록 조회
     * 
     * @param leagueId 리그 ID
     */
    // ⭐️ [수정] PathVariable 대신 QueryParam을 사용하도록 변경 (RESTful API 디자인 통일성)
    @GetMapping
    public Mono<String> getTeamsByLeague(@RequestParam("leagueId") Integer leagueId) {
        log.info("=== Team API Called: /api/v1/feed/teams?leagueId={} ===", leagueId);
        return teamService.getTeamsByLeague(leagueId);
    }
}