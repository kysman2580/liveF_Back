package com.livef.livef_memberservice.member.model.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.livef.livef_memberservice.member.model.dto.MemberDTO;
import com.livef.livef_memberservice.member.model.entity.MemberEntity;
import com.livef.livef_memberservice.member.model.repository.MemberRepository;
import com.livef.livef_memberservice.util.service.AuthFacade;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

	private final MemberRepository memberRepository;
	private final AuthFacade authFacade;
	private final PasswordEncoder passwordEncoder;
	
	@Override
	public void insertMember(MemberDTO member) {
		MemberEntity memberEntity = getMemberEntity(member);
		memberRepository.save(memberEntity);
	}

	@Override
	public Map<String, Object> selectMyInfo(Long memberNo) {
		Map<String,Object> data = new HashMap<>();
		Optional<MemberEntity> memberEntity = memberRepository.findById(memberNo);
		// 1) 없으면 예외
//		MemberEntity m = memberRepository.findById(memberNo)
//		    .orElseThrow(() -> new EntityNotFoundException("회원 없음"));
		data.put("member", memberEntity);
		return data;
	}

	@Override
	public Map<String,Object> selectMemberList() {
		Map<String,Object> data = new HashMap<>();
		List<MemberEntity> memberEntity = memberRepository.findAll();
		data.put("memberList", memberEntity);
		return data;
	}

	@Override
	public void updateMember(MemberDTO member) {
		MemberEntity existing = memberRepository.findById(member.getMemberNo())
		        .orElseThrow(() -> new RuntimeException("회원이 존재하지 않습니다."));
	}
	
	private MemberEntity getMemberEntity(MemberDTO member) {
		MemberEntity memberEntity = MemberEntity.builder()
				.memberId(member.getMemberId())
				.memberPw(passwordEncoder.encode(member.getMemberPw()))
				.memberName(member.getMemberName())
				.memberNickname(member.getMemberNickname())
				.memberPhone(member.getMemberPhone())
				.build();
		return memberEntity;
	}

	@Override
	public void deleteMember(Long memberNo) {
		memberRepository.deleteById(memberNo);
	}
	


}
