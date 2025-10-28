package org.livef.livef_dataservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TodayFixtureResponse {
    @JsonProperty("fixture")
    private Fixture fixture;

    @JsonProperty("league")
    private League league;

    @JsonProperty("teams")
    private Teams teams;

    @JsonProperty("score")
    private Score score;

    @JsonProperty("goals")
    private Goals goals;
}
