package org.livef.livef_apigateway.component;

import java.util.Optional;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
@Component
public class JwtGlobalFilter implements GlobalFilter, Ordered {

    private final JwtUtil util;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().toString();

        log.info("🛰️ [JwtGlobalFilter] {} {}", method, path);

        // ⭐ WebSocket 경로 처리 (최우선)
        if (path.startsWith("/ws")) {
            log.info("🌐 [WebSocket 경로 감지] 무조건 통과: {}", path);

            // 쿠키 전체 출력 (디버깅)
            log.info("🪀 전체 쿠키 목록: {}", exchange.getRequest().getCookies());

            // ACCESS_TOKEN 쿠키 확인
            String token = null;
            var cookies = exchange.getRequest().getCookies().get("ACCESS_TOKEN");

            if (cookies != null && !cookies.isEmpty()) {
                HttpCookie cookie = cookies.get(0);
                token = cookie.getValue();
                log.info("✅ [JwtGlobalFilter] 쿠키에서 ACCESS_TOKEN 감지");
                log.info("🔑 토큰 앞 20자: {}...", token.substring(0, Math.min(20, token.length())));
            } else {
                log.warn("⚠️ [WebSocket] ACCESS_TOKEN 쿠키 없음 → 익명으로 통과");
                return chain.filter(exchange); // ⭐ 토큰 없어도 통과
            }

            // 토큰이 있으면 파싱 시도
            if (token != null && !token.isBlank()) {
                try {
                    Claims claims = util.parseJwt(token);

                    Long memberNo = Optional.ofNullable(claims.get("memberNo", Number.class))
                            .map(Number::longValue).orElse(null);
                    String username = claims.getSubject();
                    String role = (String) claims.get("role");

                    log.info("✅ [WebSocket] JWT 파싱 성공 → 헤더 추가");
                    log.info("   memberNo: {}, username: {}, role: {}", memberNo, username, role);

                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(exchange.getRequest().mutate()
                                    .header("X-Username", username)
                                    .header("X-User-No", String.valueOf(memberNo))
                                    .header("X-User-Role", role)
                                    .build())
                            .build();

                    return chain.filter(mutatedExchange);

                } catch (JwtException e) {
                    log.warn("⚠️ [WebSocket] 유효하지 않은 토큰 감지. 익명으로 통과: {}", e.getMessage());
                    return chain.filter(exchange); // ⭐ 토큰 파싱 실패해도 통과
                } catch (Exception e) {
                    log.error("💥 [JwtGlobalFilter] JWT 필터 처리 중 오류 발생", e);
                    return chain.filter(exchange); // ⭐ 오류 발생해도 통과
                }
            }

            // 토큰이 없거나 blank인 경우
            return chain.filter(exchange);
        }

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // 비-WebSocket 경로 처리 (기존 로직 유지)
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

        String token = null;
        var cookies = exchange.getRequest().getCookies().get("ACCESS_TOKEN");

        if (cookies != null && !cookies.isEmpty()) {
            HttpCookie cookie = cookies.get(0);
            token = cookie.getValue();
        }

        if (token == null || token.isBlank()) {
            log.info("⚠️ [JwtGlobalFilter] 토큰 없음 → 인증 없이 통과 (path: {})", path);
            return chain.filter(exchange);
        }

        try {
            Claims claims = util.parseJwt(token);

            Long memberNo = Optional.ofNullable(claims.get("memberNo", Number.class))
                    .map(Number::longValue).orElse(null);
            String username = claims.getSubject();
            String role = (String) claims.get("role");

            log.info("✅ [JwtGlobalFilter] JWT 파싱 성공 → memberNo={}, username={}, role={}",
                    memberNo, username, role);

            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(exchange.getRequest().mutate()
                            .header("X-Username", username)
                            .header("X-User-No", String.valueOf(memberNo))
                            .header("X-User-Role", role)
                            .build())
                    .build();

            return chain.filter(mutatedExchange);

        } catch (ExpiredJwtException e) {
            log.warn("⏰ [JwtGlobalFilter] 만료된 토큰: {}", e.getMessage());
            return handleUnauthorized(exchange, "만료된 토큰입니다.");
        } catch (JwtException e) {
            log.warn("🚫 [JwtGlobalFilter] 유효하지 않은 토큰: {}", e.getMessage());
            return handleUnauthorized(exchange, "유효하지 않은 토큰입니다.");
        } catch (Exception e) {
            log.error("💥 [JwtGlobalFilter] JWT 필터 처리 중 오류 발생", e);
            return handleUnauthorized(exchange, "인증 처리 중 오류가 발생했습니다.");
        }
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        log.warn("❌ [JwtGlobalFilter] 요청 거부 - {}", message);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        byte[] bytes = message.getBytes();
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
        );
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}