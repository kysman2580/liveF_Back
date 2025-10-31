package org.livef.livef_authservice.auth.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

import org.livef.livef_authservice.auth.model.entity.MemberEntity;
import org.livef.livef_authservice.auth.model.service.KakaoService;
import org.livef.livef_authservice.util.dto.ResponseData;
import org.livef.livef_authservice.util.response.ResponseUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
@Slf4j
public class KakaoController {

	private final KakaoService kakaoService;
	private final ResponseUtil responseUtil;
	
	@Value("${oauth2.kakao.redirect-uri}")
	private String redirectUri;
	
    @GetMapping("/kakao/url")
    public ResponseData getKakaoLoginUrl(){
        return responseUtil.getResponseData(kakaoService.getKakaoLoginUrl(),"카카오 로그인 url 반환 완료","201");
    }
    
    @GetMapping("/kakao/callback")
    public void kakaoCallbacks(@RequestParam("code") String code, HttpServletResponse response) throws IOException {
    	log.info("카카오 인가코드: {}", code);
        try {
            // 1️⃣ 카카오 로그인 처리 및 토큰 생성
            Map<String, Object> data = kakaoService.getKakaoAcessToken(code);

            // 2️⃣ 서비스에서 반환한 쿠키 꺼내기
            ResponseCookie accessCookie  = (ResponseCookie) data.get("accessCookie");
            ResponseCookie refreshCookie = (ResponseCookie) data.get("refreshCookie");
            MemberEntity member = (MemberEntity) data.get("memberInfo");

            // 3️⃣ 응답 헤더에 쿠키 추가
            response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            // 4️⃣ 프론트로 리다이렉트 (필요하면 사용자 정보 같이 전달)
            String redirectUrl = "http://localhost:5173/oauth/success";
            redirectUrl += "?memberId=" + URLEncoder.encode(member.getMemberId(), "UTF-8");

            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("카카오 로그인 중 오류: {}", e.getMessage());
            // 실패 시 로그인 페이지로 리다이렉트
            String redirectUrl = "https://onnomnom.shop/login?error=" + URLEncoder.encode(e.getMessage(), "UTF-8");
            response.sendRedirect(redirectUrl);
        }
    }

}
