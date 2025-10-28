package com.livef.livef_memberservice.member.model.service;

import java.util.List;
import java.util.Map;

import com.livef.livef_memberservice.member.model.dto.MemberDTO;

import jakarta.validation.Valid;

public interface MemberService {
	
	// 회원가입
	void insertMember(MemberDTO member);

	// 마이페이지 조회
	Map<String, Object> selectMyInfo(Long memberNo);

	// 회원 전체 조회
	Map<String, Object> selectMemberList();

	// 회원 정보 수정
	void updateMember(MemberDTO member);

}
