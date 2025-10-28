package org.livef.livef_authservice.auth.model.vo;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;

@Value
@Builder
@Getter
@ToString
public class CustomUserDetails implements UserDetails {

    private final Long memberNo;
    private final String username; 
    private final String password; 
    private final String isActive;
    private final Collection<? extends GrantedAuthority> authorities;
}
