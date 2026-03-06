package org.livef.livef_dataservice.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Standing {
    private int rank;
    private PureTeam team;
    private int points;
    private int goalsDiff;
    private String group;
    private String form;
    private String status;
    private String description;
    private StandingStats all;
    private StandingStats home;
    private StandingStats away;
    private String update;
}
