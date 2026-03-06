package org.livef.livef_dataservice.dto;

import lombok.*;

import java.time.LocalDateTime;

// @Builder를 사용하여 ServiceImpl에서 데이터를 수동으로 쉽게 채울 수 있도록 합니다.
@Data
@Builder
public class TodayFixtureDetail {
    private Long fixtureId;
    private String leagueName;
    private String homeTeamName;
    private String awayTeamName;
    private String homeTeamLogoUrl;
    private String awayTeamLogoUrl;
    @Builder.Default
    private String status = "NS";
    private String score;
    private LocalDateTime kickoffTime;
    @Builder.Default
    private String venue = "Unknown Venue";
    private String time;
}
