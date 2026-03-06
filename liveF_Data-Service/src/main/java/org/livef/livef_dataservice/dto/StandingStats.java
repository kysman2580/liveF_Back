package org.livef.livef_dataservice.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class StandingStats {
    private int played;
    private int win;
    private int draw;
    private int lose;
    private StandingGoals goals;
}
