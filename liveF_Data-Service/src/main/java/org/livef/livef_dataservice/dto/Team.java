package org.livef.livef_dataservice.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Team {
    private int id;
    private String name;
    private String logo;
    private boolean winner; // 이 경기의 승리 여부 (승리 시 true, 패배/무승부 시 false/null)
}
