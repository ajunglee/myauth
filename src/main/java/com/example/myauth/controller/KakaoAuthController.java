package com.example.myauth.controller;

import com.example.myauth.config.AppProperties;
import com.example.myauth.dto.kakao.KakaoOAuthDto;
import com.example.myauth.dto.ApiResponse;
import com.example.myauth.dto.LoginResponse;
import com.example.myauth.service.KakaoOAuthService;
import com.example.myauth.util.ClientTypeDetector;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * 카카오 OAuth 로그인 컨트롤러
 * 카카오 소셜 로그인 엔드포인트를 제공
 */
@Slf4j
@RestController
@RequestMapping("/auth/kakao")
@RequiredArgsConstructor
public class KakaoAuthController {

  private final KakaoOAuthService kakaoOAuthService;
  private final AppProperties appProperties;

  /**
   * 카카오 로그인 시작
   * 사용자를 카카오 로그인 페이지로 리다이렉트
   *
   * GET /auth/kakao/login
   */
  @GetMapping("/login")
  public void kakaoLogin(HttpServletResponse response) throws IOException {
    log.info("카카오 로그인 요청");

    // 카카오 인가 코드 요청 URL 생성
    String authorizationUrl = kakaoOAuthService.getAuthorizationUrl();

    log.info("카카오 인가 페이지로 리다이렉트: {}", authorizationUrl);

    // 카카오 로그인 페이지로 리다이렉트
    response.sendRedirect(authorizationUrl);
  }

  /**
   * 카카오 로그인 콜백 처리 (하이브리드 방식 - 웹/모바일 구분)
   * 카카오 인증 후 Authorization Code를 받아 JWT 발급
   * 클라이언트 타입에 따라 토큰 전송 방식을 다르게 처리:
   * - 웹 브라우저: Refresh Token을 HTTP-only 쿠키로 전송 (XSS 방어)
   * - 모바일 앱: 모든 토큰을 JSON 응답 바디로 전송
   *
   * GET /auth/kakao/callback?code=AUTHORIZATION_CODE
   *
   * @param code 카카오 인가 코드
   * @param request HTTP 요청 객체 (클라이언트 타입 감지용)
   * @param response HTTP 응답 객체 (쿠키 설정용)
   * @return 로그인 응답 (JWT 포함)
   */
  @GetMapping("/callback")
  public ResponseEntity<ApiResponse<LoginResponse>> kakaoCallback(
      @RequestParam String code,
      HttpServletRequest request,
      HttpServletResponse response
  ) {
    log.info("카카오 로그인 콜백 - code: {}", code);

    try {
      // 1️⃣ 클라이언트 타입 감지
      boolean isWebClient = ClientTypeDetector.isWebClient(request);
      String clientType = ClientTypeDetector.getClientTypeString(request);
      log.info("감지된 클라이언트 타입: {}", clientType);

      // 2️⃣ Authorization Code로 카카오 Access Token 요청
      KakaoOAuthDto.TokenResponse tokenResponse = kakaoOAuthService.getAccessToken(code);
      log.info("카카오 Access Token 발급 완료");

      // 3️⃣ 카카오 Access Token으로 사용자 정보 조회
      KakaoOAuthDto.UserInfoResponse kakaoUserInfo = kakaoOAuthService.getUserInfo(tokenResponse.getAccessToken());
      log.info("카카오 사용자 정보 조회 완료 - 카카오 ID: {}", kakaoUserInfo.getId());

      // 4️⃣ 카카오 사용자 정보로 로그인 처리 (자동 회원가입 포함)
      LoginResponse loginResponse = kakaoOAuthService.processKakaoLogin(kakaoUserInfo);
      log.info("카카오 로그인 성공 - User ID: {}", loginResponse.getUser().getId());

      // 5️⃣ 웹 클라이언트면 Refresh Token을 쿠키로 설정
      if (isWebClient) {
        log.info("웹 클라이언트 감지 → Refresh Token을 HTTP-only 쿠키로 설정");

        // Refresh Token을 HTTP-only 쿠키로 설정
        Cookie refreshTokenCookie = new Cookie("refreshToken", loginResponse.getRefreshToken());
        refreshTokenCookie.setHttpOnly(true);   // JavaScript 접근 불가 (XSS 방어)
        refreshTokenCookie.setSecure(appProperties.getCookie().isSecure());  // 환경별 동적 설정 (개발: false, 프로덕션: true)
        log.info("쿠키 Secure 플래그: {}", appProperties.getCookie().isSecure());
        refreshTokenCookie.setPath("/");        // 모든 경로에서 쿠키 전송
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7일 (초 단위)

        response.addCookie(refreshTokenCookie);
        log.info("Refresh Token을 쿠키에 설정 완료");

        // 응답 바디에서 Refresh Token 제거 (쿠키로 전송했으므로)
        loginResponse.setRefreshToken(null);
        log.info("응답 바디에서 Refresh Token 제거 (보안 강화)");
      } else {
        // 6️⃣ 모바일 클라이언트면 Refresh Token을 JSON 응답에 포함
        log.info("모바일 클라이언트 감지 → Refresh Token을 JSON 응답에 포함");
      }

      // 7️⃣ 로그인 성공 응답 반환
      log.info("카카오 로그인 성공: {}, 클라이언트: {}", loginResponse.getUser().getEmail(), clientType);
      return ResponseEntity.ok(ApiResponse.success("카카오 로그인 성공", loginResponse));

    } catch (Exception e) {
      log.error("카카오 로그인 실패: {}", e.getMessage(), e);
      return ResponseEntity.status(500).body(ApiResponse.error("카카오 로그인 실패: " + e.getMessage()));
    }
  }
}
