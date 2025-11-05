package org.livef.livef_authservice.auth.model.service;

import java.util.HashMap;
import java.util.Map;

import javax.management.RuntimeErrorException;

import org.livef.livef_authservice.auth.model.entity.MemberEntity;
import org.livef.livef_authservice.auth.model.repository.MemberRepository;
import org.livef.livef_authservice.exception.FailedKakaoTokenRequestException;
import org.livef.livef_authservice.exception.FailedParsingInfoException;
import org.livef.livef_authservice.exception.FailedRequestInfoException;
import org.livef.livef_authservice.exception.InvalidTokenException;
import org.livef.livef_authservice.exception.KakaoParsingException;
import org.livef.livef_authservice.token.model.service.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class KakaoServiceImpl implements KakaoService {

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;
	
	@Value("${oauth2.kakao.base-url}")
	private String baseUrl;

	@Value("${oauth2.kakao.client-id}")
	private String clientId;
	
	@Value("${oauth2.kakao.client-secret}")
	private String clientSecret;

	@Value("${oauth2.kakao.redirect-uri}")
	private String redirectUri;
	
	@Override
	public Map<String, Object> getKakaoLoginUrl() {
	    try {
            // https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=${REST_API_KEY}&redirect_uri=${REDIRECT_URI}&prompt=login
            String kakaoLoginUrl = baseUrl + "?response_type=code" + "&client_id=" + clientId + "&redirect_uri=" + redirectUri + "&prompt=login";
            Map<String, Object> data = new HashMap<>();
            data.put("loginUrl", kakaoLoginUrl);
            return data;
        } catch (Exception e) {
            throw new RuntimeErrorException(null, "에러발생");
        }
    }
	
	
	// 여기서 코드로 토큰 받아오는 요청 보냄
	@Override
	public Map<String, Object> getKakaoAcessToken(String code) {
	    
		String tokenUrl = "https://kauth.kakao.com/oauth/token";
	 	// 헤더설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        // 요청 바디 설정
        MultiValueMap<String, String> body = new LinkedMultiValueMap();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
        String accessToken;
        // 요청 보내기
        
        try {
           
        	ResponseEntity<String> response = restTemplate.exchange(
        			tokenUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            if (response.getStatusCode() == HttpStatus.OK){
                try {
                	JsonNode jsonNode = objectMapper.readTree(response.getBody());
                    accessToken = jsonNode.get("access_token").asText();
                } catch (JsonProcessingException e) {
                    throw new KakaoParsingException("카카오 토큰 파싱 오류");
                }
            } else {
                throw new FailedKakaoTokenRequestException("카카오 토큰 요청 실패");
            }
        } catch (Exception e){
            throw new InvalidTokenException("카카오 토큰 요청 중 오류 발생");
        }
        return insertKakaoUser(accessToken);

    }
	
	// accessToken으로 사용자 정보 조회
	// => DB에 이 사람이 회원이면 바로 로그인, 비회원이면 회원가입 후 로그인처리
	private Map<String, Object> insertKakaoUser(String accessToken){
        log.info("emailㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁ {}", accessToken);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://kapi.kakao.com/v2/user/me",
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );
            log.info("emailㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁㅁ {}", response);
            // 응답 처리
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                
                String id = jsonNode.get("id").asText();
                log.info("tqtqtqtqtqtqtqtqtqtqq id가 널이야? {}", id);
                MemberEntity memberEntity = memberRepository.findByMemberId(id);
                if (memberEntity == null){
                	MemberEntity member = MemberEntity
                                            .builder()
                                            .memberId(id)
                                            .memberPw(passwordEncoder.encode(id))
                                            .memberPhone(id)
                                            .memberName(id)
                                            .memberNickname(id)
                                            .build();
                    memberRepository.save(member);
                    return kakaoLoginSection(member);
                }
                return kakaoLoginSection(memberEntity);
            } else {
                throw new FailedRequestInfoException("카카오 사용자 정보 요청 실패");
            }
        } catch (JsonProcessingException e) {
            throw new FailedParsingInfoException("카카오 사용자 정보 파싱 오류: ");
        }
    }
	
	// 회원정보를 바탕으로 로그인 ( 토큰 발급 및 사용자 정보 리턴 )
	private Map<String, Object> kakaoLoginSection(MemberEntity memberEntity) {
		 
		Map<String, Object> data = new HashMap<>();
		Map<String, Object> token = tokenService.generateToken(memberEntity.getMemberId(), memberEntity.getMemberNo());
		
	    String accessToken  = (String) token.get("accessToken");
	    String refreshToken = (String) token.get("refreshToken");
	    data.put("accessCookie",  buildCookie("ACCESS_TOKEN",  accessToken,  15 * 60));
	    data.put("refreshCookie", buildCookie("REFRESH_TOKEN", refreshToken, 7 * 24 * 60 * 60));
		data.put("memberInfo",memberEntity);
		return data;
	}


	private ResponseCookie buildCookie(String name, String token, int maxAgeSeconds) {
	    return ResponseCookie.from(name, token)
	        .path("/")
	        .maxAge(maxAgeSeconds)
	        .httpOnly(true)
	        .secure(false)        // 로컬 http 개발이면 false
	        .sameSite("Lax")    // 크로스도메인일 때 필요
	        .build();
	}
	
}
