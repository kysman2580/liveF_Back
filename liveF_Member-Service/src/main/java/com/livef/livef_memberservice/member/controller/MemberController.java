package com.livef.livef_memberservice.member.controller;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.livef.livef_memberservice.member.model.dto.MemberDTO;
import com.livef.livef_memberservice.member.model.dto.MemberUpdateDTO;
import com.livef.livef_memberservice.member.model.service.MemberService;
import com.livef.livef_memberservice.util.dto.ResponseData;
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
	public ResponseEntity<ResponseData> insertMember(@RequestBody @Valid MemberDTO member) {
		log.info("member : {}",member);
		memberService.insertMember(member);
		return ResponseEntity.ok(responseUtil.getResponseData("회원가입 되었습니다.", "201"));
	}
	
	// 마이페이지 조회
	@GetMapping("/myInfo")
	public ResponseEntity<ResponseData> selectMyInfo(
			@RequestHeader(value="X-User-No", required=false) Long memberNo,
			@RequestHeader HttpHeaders headers) { // 👈 모든 헤더 추가 수신

		log.info(">>> [DEBUG: INBOUND] MemberService 요청 도착");
		log.info(">>> X-User-No 헤더 값: {}", memberNo);

		// 요청 헤더 전체 로깅 (Cookie가 전달된다면 여기서 확인 가능)
		headers.forEach((key, value) -> {
			log.info(">>> Header: {} = {}", key, value);
		});

		// 만약 memberNo가 null이면 401 오류가 발생해야 하지만,
		// 이 메서드가 호출되었다는 것은 이미 인증을 통과했거나 Gateway가 오류 처리 전입니다.
		// MemberNo가 null이면 바로 오류를 던져야 합니다. (현재 코드는 required=false)
		if (memberNo == null) {
			log.error("X-User-No 헤더가 누락되어 인증 실패!");
			// 실제로는 인증 필터에서 막히므로, 이 로그가 찍히면 Gateway 설정 오류임.
			throw new RuntimeException("인증되지 않은 접근"); // 적절한 예외 처리로 변경
		}

		Map<String, Object> data = memberService.selectMyInfo(memberNo);
		return ResponseEntity.ok(responseUtil.getResponseData(data, "내 정보가 조회되었습니다.", "200"));
	}
	
	// 회원정보 전체 조회
	@GetMapping("/memberList")
	public ResponseEntity<ResponseData> selectMemberList() {
		Map<String, Object> data = memberService.selectMemberList();
		return ResponseEntity.ok(responseUtil.getResponseData("회원 전체 조회되었습니다.", "200"));
	}
	
	// 아이디 체크
	@GetMapping("/check-id")
	public ResponseEntity<?> selectCheckId(@RequestParam("memberId") String memberId) {
		log.info("memberId : {}",memberId);
		memberService.selectCheckId(memberId);
		return ResponseEntity.ok(responseUtil.getResponseData("아이디가 확인되었습니다.", "200"));
	}
	
	// 비밀번호 변경
	@PostMapping("/change-password")
	public ResponseEntity<?> changePassword(@RequestBody @Valid MemberDTO member) {
	    memberService.changePassword(member.getMemberId(), member.getMemberPw());
		return ResponseEntity.ok(responseUtil.getResponseData("비밀번호가 변경되었습니다.", "200"));
	}
	
	// 회원정보 수정
	@PutMapping("/update")
	public ResponseEntity<ResponseData> updateMember(@RequestBody @Valid MemberUpdateDTO member) {
		 log.info("member :{}",member);
		 memberService.updateMember(member);
		 return ResponseEntity.ok(responseUtil.getResponseData("회원정보 수정이 완료되었습니다.", "201"));
	}
	
	// 회원 탈퇴
	@DeleteMapping("/delete")
	public ResponseEntity<ResponseData> deleteMember(@RequestHeader(value="X-User-No", required=false) Long memberNo) {
		 memberService.deleteMember(memberNo);
		 ResponseCookie accessCookie = ResponseCookie.from("ACCESS_TOKEN", "")
						        .path("/")
						        .maxAge(0)
						        .httpOnly(true)
						        .secure(true)        // 로컬 http 개발이면 false
						        .sameSite("None")    // 크로스도메인일 때 필요
						        .build();
		 ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", "")
				 .path("/")
				 .maxAge(0)
				 .httpOnly(true)
				 .secure(true)        // 로컬 http 개발이면 false
				 .sameSite("None")    // 크로스도메인일 때 필요
				 .build();
		 return ResponseEntity.ok()
		            .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
		            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
		            .body(responseUtil.getResponseData("회원탈퇴 되었습니다.", "201"));
	}
}
