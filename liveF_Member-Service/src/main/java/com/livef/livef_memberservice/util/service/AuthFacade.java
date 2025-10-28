package com.livef.livef_memberservice.util.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.livef.livef_memberservice.util.vo.CustomUserDetails;

@Component
public class AuthFacade { // authServiceImpl 같은 역할
    public CustomUserDetails getUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
//            throw new UnauthenticatedException();
//        }
        return (CustomUserDetails) auth.getPrincipal();
    }

    public Long getMemberNo() {
        return getUserDetails().getMemberNo();
    }
}