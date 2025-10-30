package com.livef.livef_memberservice.member.model.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.livef.livef_memberservice.exception.DuplicateMemberIdException;
import com.livef.livef_memberservice.member.model.dto.MemberDTO;
import com.livef.livef_memberservice.member.model.dto.MemberUpdateDTO;
import com.livef.livef_memberservice.member.model.entity.MemberEntity;
import com.livef.livef_memberservice.member.model.repository.MemberRepository;
import com.livef.livef_memberservice.util.service.AuthFacade;

import jakarta.persistence.EntityNotFoundException;
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
		try {
		    memberRepository.save(memberEntity);
		} catch (DataIntegrityViolationException e) {
		    throw new DuplicateMemberIdException("이미 존재하는 아이디입니다.");
		}
	}

	@Override
	public Map<String, Object> selectMyInfo(Long memberNo) {
		Map<String,Object> data = new HashMap<>();
		MemberEntity memberEntity = memberRepository.findById(memberNo)
		    .orElseThrow(() -> new EntityNotFoundException("회원 없음"));
		memberEntity.setMemberPw(null);
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
	public void updateMember(MemberUpdateDTO member) {
		log.info("member : {}",member);
		MemberEntity existing = memberRepository.findById(member.getMemberNo())
		        .orElseThrow(() -> new RuntimeException("회원이 존재하지 않습니다."));
		
		existing.setMemberId(member.getMemberId());
		existing.setMemberName(member.getMemberName());
	    existing.setMemberNickname(member.getMemberNickname());
	    existing.setMemberPhone(member.getMemberPhone());
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
