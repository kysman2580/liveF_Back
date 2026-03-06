package org.livef.livef_dataservice.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.util.List;

@Getter
@Setter
@ToString
public class LeagueStanding {
    private League league;
    private List<List<Standing>> standings;
}
