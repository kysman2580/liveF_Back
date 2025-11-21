package org.livef.livef_dataservice.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PureTeam {
    private int id;
    private String name;
    private String logo;
    // winner 필드 제외
}