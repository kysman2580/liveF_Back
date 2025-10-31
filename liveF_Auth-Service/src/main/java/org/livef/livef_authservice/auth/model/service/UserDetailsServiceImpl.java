package org.livef.livef_authservice.auth.model.service;

import java.util.Collections;
import java.util.Optional;

import org.livef.livef_authservice.auth.model.dto.MemberDTO;
import org.livef.livef_authservice.auth.model.entity.MemberEntity;
import org.livef.livef_authservice.auth.model.repository.MemberRepository;
import org.livef.livef_authservice.auth.model.vo.CustomUserDetails;
import org.livef.livef_authservice.exception.CustomAuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService{
	
	//private final SocialMemberMapper socialMemberMapper;
	private final MemberRepository memberRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
	    System.out.println("[UserDetailsServiceImpl] 입력된 username: [" + username + "]");

	    // 1. 일반 회원 조회
        MemberEntity member = memberRepository.findByMemberId(username);
        if(member == null) {
        	throw new UsernameNotFoundException("No such user: " + username);
        }
        log.info("[UserDetailsServiceImpl] 일반 회원 조회 결과: {}", member);

	        return CustomUserDetails.builder()
	                .memberNo(member.getMemberNo())
	                .username(member.getMemberId())
	                .password(member.getMemberPw())  // 일반 회원의 경우 비밀번호 반환
	                .isActive(member.getIsActive())
	                .authorities(Collections.singletonList(new SimpleGrantedAuthority(member.getMemberRole())))
	                .build();
	    }

//	    // 3. 일반 회원이 없으면 소셜 회원 조회
//	    log.info("[UserDetailsServiceImpl] 소셜 회원 조회 시작");
//	    SocialMemberDTO socialMember = socialMemberMapper.findByEmail(username);
//	    log.info("[UserDetailsServiceImpl] 소셜 회원 조회 결과: {}", socialMember);
//
//	    // 4. 소셜 회원이 존재하면 소셜 로그인 정보 사용
//	    if (socialMember != null) {
//	        // 소셜 회원의 닉네임을 사용하여 반환
//	        return CustomUserDetails.builder()
//	                .memberNo(socialMember.getMemberNo())
//	                .username(socialMember.getEmail())
//	                .password(null)  // 소셜 회원은 비밀번호가 없으므로 null
//	                .memberName(socialMember.getNickName())  // 소셜 로그인 시 소셜 닉네임 사용
//	                .memberStatus(socialMember.getMemberStatus())
//	                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + socialMember.getRole())))
//	                .build();
//	    }

//	    log.error("[UserDetailsServiceImpl] 사용자 조회 실패 - 존재하지 않는 사용자: {}", cleanUsername);
//	    throw new CustomAuthenticationException("존재하지 않는 사용자입니다.");
	}



