package org.livef.livef_dataservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class StandingGoals {
    @JsonProperty("for")
    private int goalsFor;
    @JsonProperty("against")
    private int goalsAgainst;
}
