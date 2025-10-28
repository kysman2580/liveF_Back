package com.livef.livef_memberservice.member.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.livef.livef_memberservice.member.model.dto.MemberDTO;
import com.livef.livef_memberservice.member.model.service.MemberService;
import com.livef.livef_memberservice.util.response.ResponseUtil;
import com.livef.livef_memberservice.util.vo.CustomUserDetails;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
@Slf4j
public class MemberController {

	private final MemberService memberService;
	private final ResponseUtil responseUtil;
	
	// 회원가입
	@PostMapping("/sign-up")
	public ResponseEntity<?> insertMember(@RequestBody @Valid MemberDTO member) {
		log.info("member : {}",member);
		memberService.insertMember(member);
		return ResponseEntity.ok(responseUtil.getResponseData("회원가입 되었습니다.", "201"));
	}
	
	// 마이페이지 조회
	@GetMapping("/myInfo")
	public ResponseEntity<?> selectMyInfo(@AuthenticationPrincipal CustomUserDetails member) {
		Map<String, Object> data = memberService.selectMyInfo(member.getMemberNo());
		return ResponseEntity.ok(responseUtil.getResponseData(data, "내 정보가 조회되었습니다.", "200"));
	}
	
	// 회원정보 전체 조회
	@GetMapping("/memberList")
	public ResponseEntity<?> selectMemberList() {
		Map<String, Object> data = memberService.selectMemberList();
		return ResponseEntity.ok(responseUtil.getResponseData("회원 전체 조회되었습니다.", "200"));
	}
	
//	// 아이디 or 닉네임 중복 체크
//	@GetMapping("/check")
//	public ResponseEntity<?> selectCheck( );
	
	// 회원정보 수정
	@PutMapping("/update")
	public ResponseEntity<?> updateMember(@RequestBody @Valid MemberDTO member) {
		 memberService.updateMember(member);
		 return ResponseEntity.ok(responseUtil.getResponseData("회원정보 수정이 완료되었습니다.", "201"));
	}
	
//	// 회원 탈퇴
//	@DeleteMapping("/delete")
//	public ResponseEntity<?> deleteMember();
}
