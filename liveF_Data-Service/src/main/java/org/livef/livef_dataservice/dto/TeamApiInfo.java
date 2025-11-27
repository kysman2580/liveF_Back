package org.livef.livef_dataservice.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TeamApiInfo {
    private PureTeam team; // PureTeam 사용
    private Venue venue;   // 기존 Venue.java 재사용
}