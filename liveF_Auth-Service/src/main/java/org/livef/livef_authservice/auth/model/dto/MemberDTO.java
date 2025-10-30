package org.livef.livef_authservice.auth.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class MemberDTO {
	
	private Long memberNo;
    @NotBlank(message = "아이디는 필수 입력값입니다.")
    @Pattern(
        regexp = "^(?=[a-z]*[a-z0-9])[a-z0-9]{4,20}$",
        message = "아이디는 소문자 영문과 숫자를 포함하여 4~20자 이내여야 합니다. 숫자만으로는 구성할 수 없습니다."
    )
    private String memberId;

    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
    @Pattern(
        regexp = "^(?=(.*[a-z]){1,})(?=(.*[A-Z]){1,})(?=(.*\\d){1,})(?=(.*[!@#$%^&*()_+=-]){1,}).{8,20}$",
        message = "비밀번호는 대소문자, 숫자, 특수문자 중 3가지 이상을 포함해야 합니다."
    )
    private String memberPw;
    private String isActive;
    private String MemberRole;
}