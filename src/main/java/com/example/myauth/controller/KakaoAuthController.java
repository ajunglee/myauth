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
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * 카카오 OAuth 로그인 컨트롤러
 * 카카오 소셜 로그인 엔드포인트를 제공
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/kakao")
@RequiredArgsConstructor
public class KakaoAuthController {

  private final KakaoOAuthService kakaoOAuthService;
  private final AppProperties appProperties;

  /**
   * 토큰 교환 엔드포인트 (Cross-Port 쿠키 문제 해결용)
   * OAuth callback에서 세션에 저장한 토큰을 가져와 HTTP-only 쿠키로 설정
   * 이 엔드포인트는 프론트엔드가 Vite 프록시를 통해 호출하므로 쿠키가 정상 작동함
   *
   * POST /auth/kakao/exchange-token
   *
   * @param request HTTP 요청 객체 (세션에서 토큰 가져오기)
   * @param response HTTP 응답 객체 (쿠키 설정)
   * @return Access Token과 사용자 정보
   */
  @PostMapping("/exchange-token")
  public ResponseEntity<ApiResponse<LoginResponse>> exchangeToken(
      HttpServletRequest request,
      HttpServletResponse response
  ) {
    log.info("토큰 교환 요청");

    // 1️⃣ 세션에서 대기 중인 LoginResponse 가져오기
    HttpSession session = request.getSession(false);
    if (session == null) {
      log.warn("세션이 없음 - 토큰 교환 실패");
      return ResponseEntity
          .status(401)
          .body(ApiResponse.error("세션이 만료되었습니다. 다시 로그인해주세요."));
    }

    LoginResponse loginResponse = (LoginResponse) session.getAttribute("pendingLoginResponse");
    if (loginResponse == null) {
      log.warn("세션에 pendingLoginResponse가 없음 - 토큰 교환 실패");
      return ResponseEntity
          .status(401)
          .body(ApiResponse.error("로그인 정보가 없습니다. 다시 로그인해주세요."));
    }

    // 2️⃣ 세션에서 제거 (일회용)
    session.removeAttribute("pendingLoginResponse");
    log.info("세션에서 pendingLoginResponse 제거 완료");

    // 3️⃣ Refresh Token을 HTTP-only 쿠키로 설정
    // ResponseCookie를 사용하여 SameSite와 Domain 속성 명시
    // - SameSite=Lax: CSRF 방어 + 일반적인 웹 사용 가능
    // - Domain=localhost: 포트 무관하게 모든 localhost에서 쿠키 공유 (localhost:5173과 localhost:9080 모두 접근 가능)
    ResponseCookie refreshTokenCookie = ResponseCookie
        .from("refreshToken", loginResponse.getRefreshToken())
        .httpOnly(true)   // JavaScript 접근 불가 (XSS 방어)
        .secure(appProperties.getCookie().isSecure())  // 환경별 동적 설정 (개발: false, 프로덕션: true)
        .path("/")        // 모든 경로에서 쿠키 전송
        .maxAge(7 * 24 * 60 * 60)  // 7일 (초 단위)
        .sameSite("Lax")  // CSRF 방어 + 일반 네비게이션에서 쿠키 전송 허용
        .domain("localhost")  // 포트 무관하게 localhost 전체에서 쿠키 공유
        .build();

    log.info("쿠키 설정: HttpOnly=true, Secure={}, Path=/, MaxAge=7일, SameSite=Lax, Domain=localhost",
        appProperties.getCookie().isSecure());

    response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
    log.info("Refresh Token을 쿠키에 설정 완료");

    // 4️⃣ 응답 바디에서 Refresh Token 제거 (보안 강화)
    loginResponse.setRefreshToken(null);
    log.info("응답 바디에서 Refresh Token 제거 (쿠키로만 전송)");

    // 5️⃣ Access Token과 사용자 정보 반환
    log.info("토큰 교환 성공 - User: {}", loginResponse.getUser().getEmail());
    return ResponseEntity.ok(ApiResponse.success("토큰 교환 성공", loginResponse));
  }

  /**
   * 카카오 로그인 시작
   * 사용자를 카카오 로그인 페이지로 리다이렉트
   *
   * GET /auth/kakao/login?redirectUrl=프론트엔드_콜백_URL
   *
   * @param redirectUrl 카카오 로그인 완료 후 리다이렉트할 프론트엔드 URL (선택적)
   * @param session HTTP 세션 (redirectUrl 저장용)
   * @param response HTTP 응답 객체 (리다이렉트용)
   */
  @GetMapping("/login")
  public void kakaoLogin(
      @RequestParam(required = false) String redirectUrl,
      HttpSession session,
      HttpServletResponse response
  ) throws IOException {
    log.info("카카오 로그인 요청 - redirectUrl: {}", redirectUrl);

    // 프론트엔드에서 전달한 redirectUrl을 세션에 저장
    // 카카오 OAuth 플로우 완료 후 콜백 처리 시 사용됨
    if (redirectUrl != null && !redirectUrl.isBlank()) {
      session.setAttribute("kakaoRedirectUrl", redirectUrl);
      log.info("프론트엔드 redirectUrl을 세션에 저장: {}", redirectUrl);
    } else {
      log.info("redirectUrl이 없으므로 기본 설정값을 사용합니다.");
    }

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
   * - 웹 브라우저: Refresh Token을 HTTP-only 쿠키로 전송하고 프론트엔드로 리다이렉트 (XSS 방어)
   * - 모바일 앱: 모든 토큰을 JSON 응답 바디로 전송
   *
   * GET /auth/kakao/callback?code=AUTHORIZATION_CODE
   *
   * @param code 카카오 인가 코드
   * @param request HTTP 요청 객체 (클라이언트 타입 감지용)
   * @param response HTTP 응답 객체 (쿠키 설정 및 리다이렉트용)
   * @return 모바일: 로그인 응답 (JWT 포함) / 웹: 프론트엔드로 리다이렉트
   */
  @GetMapping("/callback")
  public void kakaoCallback(
      @RequestParam String code,
      HttpServletRequest request,
      HttpServletResponse response
  ) throws IOException {
    log.info("카카오 로그인 콜백 - code: {}", code);

    try {
      // 0️⃣ 세션에서 프론트엔드 redirectUrl 가져오기
      HttpSession session = request.getSession(false);
      String frontendRedirectUrl = null;

      if (session != null) {
        frontendRedirectUrl = (String) session.getAttribute("kakaoRedirectUrl");
        if (frontendRedirectUrl != null) {
          log.info("세션에서 프론트엔드 redirectUrl 복원: {}", frontendRedirectUrl);
          // 사용 후 세션에서 제거 (보안 및 메모리 관리)
          session.removeAttribute("kakaoRedirectUrl");
        }
      }

      // redirectUrl이 없으면 기본 설정값 사용
      if (frontendRedirectUrl == null || frontendRedirectUrl.isBlank()) {
        frontendRedirectUrl = appProperties.getOauth().getKakaoRedirectUrl();
        log.info("세션에 redirectUrl이 없어 기본 설정값 사용: {}", frontendRedirectUrl);
      }

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

      // 5️⃣ 웹 클라이언트면 토큰을 URL fragment로 전달하고 프론트엔드로 리다이렉트
      if (isWebClient) {
        log.info("웹 클라이언트 감지 → 토큰을 URL fragment로 전달하고 프론트엔드로 리다이렉트");

        // 🔒 Cross-Port 세션 쿠키 문제 해결:
        // 세션 쿠키는 포트가 다르면 공유되지 않으므로, 토큰을 URL fragment(#)로 전달
        // URL fragment는 서버로 전송되지 않아 보안적으로 안전하며,
        // 프론트엔드 JavaScript에서만 접근 가능

        // 사용자 정보를 URL-safe하게 인코딩
        String userJson = String.format(
            "{\"id\":%d,\"email\":\"%s\",\"name\":\"%s\",\"profileImage\":%s}",
            loginResponse.getUser().getId(),
            loginResponse.getUser().getEmail(),
            loginResponse.getUser().getName(),
            loginResponse.getUser().getProfileImage() != null
                ? "\"" + loginResponse.getUser().getProfileImage() + "\""
                : "null"
        );
        String encodedUser = java.net.URLEncoder.encode(userJson, "UTF-8");

        // URL fragment로 토큰과 사용자 정보 전달
        // fragment는 브라우저 히스토리에 남지 않도록 프론트엔드에서 즉시 처리 권장
        String successRedirectUrl = String.format(
            "%s#accessToken=%s&user=%s",
            frontendRedirectUrl,
            loginResponse.getAccessToken(),
            encodedUser
        );

        log.info("프론트엔드로 리다이렉트 (URL fragment 사용): {}", frontendRedirectUrl);
        response.sendRedirect(successRedirectUrl);
      } else {
        // 6️⃣ 모바일 클라이언트면 JSON 응답 반환
        log.info("모바일 클라이언트 감지 → JSON 응답 반환");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // JSON 응답 작성
        String jsonResponse = String.format(
            "{\"success\":true,\"message\":\"카카오 로그인 성공\",\"data\":{\"accessToken\":\"%s\",\"refreshToken\":\"%s\",\"user\":{\"id\":%d,\"email\":\"%s\",\"name\":\"%s\"}}}",
            loginResponse.getAccessToken(),
            loginResponse.getRefreshToken(),
            loginResponse.getUser().getId(),
            loginResponse.getUser().getEmail(),
            loginResponse.getUser().getName()
        );
        response.getWriter().write(jsonResponse);
      }

      log.info("카카오 로그인 성공: {}, 클라이언트: {}", loginResponse.getUser().getEmail(), clientType);

    } catch (Exception e) {
      log.error("카카오 로그인 실패: {}", e.getMessage(), e);

      // 에러 발생 시 사용할 redirectUrl 결정 (세션 또는 기본값)
      HttpSession session = request.getSession(false);
      String errorRedirectUrl = null;

      if (session != null) {
        errorRedirectUrl = (String) session.getAttribute("kakaoRedirectUrl");
        if (errorRedirectUrl != null) {
          session.removeAttribute("kakaoRedirectUrl");
        }
      }

      if (errorRedirectUrl == null || errorRedirectUrl.isBlank()) {
        errorRedirectUrl = appProperties.getOauth().getKakaoRedirectUrl();
      }

      // 에러 발생 시 프론트엔드로 리다이렉트 (에러 메시지 포함)
      String finalErrorRedirectUrl = String.format("%s?error=%s",
          errorRedirectUrl,
          java.net.URLEncoder.encode(e.getMessage(), "UTF-8")
      );
      log.info("에러 발생 - 프론트엔드로 리다이렉트: {}", finalErrorRedirectUrl);
      response.sendRedirect(finalErrorRedirectUrl);
    }
  }
}
