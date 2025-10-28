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

import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/feed/fixtures")
@Slf4j
public class TodayFixtureController {

    private final FixtureFeedService fixtureFeedService;

    @GetMapping
    public ResponseEntity<List<TodayFixtureDetail>> getTodayFixturesByLeague(@RequestParam(name="leagueId") int leagueId) {
        log.info("📥 ===== GET /api/v1/feed/fixtures?leagueId={} called =====", leagueId);

        try {
            List<TodayFixtureDetail> fixtures = fixtureFeedService.getThreeDayFixturesByLeague(leagueId);
            log.info("✅ Service returned {} fixtures", fixtures.size());
            log.info("✅ Returning 200 OK with {} fixtures", fixtures.size());
            return ResponseEntity.ok(fixtures);
        } catch (Exception e) {
            log.error("❌❌❌ CRITICAL ERROR in getTodayFixtures ❌❌❌", e);
            log.error("❌ Error type: {}", e.getClass().getName());
            log.error("❌ Error message: {}", e.getMessage());

            // 스택 트레이스 상위 10줄 출력
            StackTraceElement[] stackTrace = e.getStackTrace();
            for (int i = 0; i < Math.min(10, stackTrace.length); i++) {
                log.error("  at {}", stackTrace[i]);
            }

            // 빈 리스트 반환 (500 에러 대신)
            return ResponseEntity.ok(Collections.emptyList());
        }
    }
}