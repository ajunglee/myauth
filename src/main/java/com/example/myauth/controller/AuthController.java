package com.example.myauth.controller;

import com.example.myauth.dto.ApiResponse;
import com.example.myauth.dto.LoginRequest;
import com.example.myauth.dto.LoginResponse;
import com.example.myauth.dto.SignupRequest;
import com.example.myauth.dto.TokenRefreshRequest;
import com.example.myauth.dto.TokenRefreshResponse;
import com.example.myauth.service.AuthService;
import com.example.myauth.util.ClientTypeDetector;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;

  @GetMapping("/health")
  public ResponseEntity<ApiResponse<Void>> health() {
    return ResponseEntity.ok(ApiResponse.success("Auth Service is running"));
  }


  @PostMapping("/signup")
  public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest signupRequest) {
    log.info("다음 이메일로 회원가입 요청: {}", signupRequest.getEmail());

    // 회원가입 처리 시도 - 기본 검증은 @Valid로 이미 완료된 상태
    ApiResponse<Void> response = authService.registerUser(signupRequest);

    HttpStatusCode statusCode = response.getSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
    return ResponseEntity.status(statusCode).body(response);
  }


  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
    log.info("로그인 요청: {}", loginRequest.getEmail());

    // 로그인 처리 - 기본 검증은 @Valid로 이미 완료된 상태
    LoginResponse response = authService.login(loginRequest);

    // 성공 시 200 OK, 실패 시 400 Bad Request
    HttpStatusCode statusCode = response.getSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
    return ResponseEntity.status(statusCode).body(response);
  }


  /**
   * 로그인 (하이브리드 방식 - 웹/모바일 구분)
   *
   * CustomUserDetailsService를 사용하여 Spring Security 표준 방식으로 로그인 처리
   * 클라이언트 타입에 따라 토큰 전송 방식을 다르게 처리한다:
   * - 웹 브라우저: Refresh Token을 HTTP-only 쿠키로 전송 (XSS 방어)
   * - 모바일 앱: 모든 토큰을 JSON 응답 바디로 전송
   *
   * @param loginRequest 로그인 요청 (email, password)
   * @param request HTTP 요청 객체 (클라이언트 타입 감지용)
   * @param response HTTP 응답 객체 (쿠키 설정용)
   * @return 로그인 응답
   */
  @PostMapping("/loginEx")
  public ResponseEntity<?> loginEx(
      @Valid @RequestBody LoginRequest loginRequest,
      HttpServletRequest request,
      HttpServletResponse response
  ) {
    log.info("로그인 요청 (loginEx): {}", loginRequest.getEmail());

    // 1️⃣ 클라이언트 타입 감지
    boolean isWebClient = ClientTypeDetector.isWebClient(request);
    String clientType = ClientTypeDetector.getClientTypeString(request);
    log.info("감지된 클라이언트 타입: {}", clientType);

    // 디버깅: User-Agent 정보 로깅
    ClientTypeDetector.logUserAgent(request);

    // 2️⃣ 로그인 처리 (CustomUserDetailsService 사용)
    LoginResponse loginResponse = authService.loginEx(loginRequest);

    // 3️⃣ 로그인 실패 시 바로 반환
    if (!loginResponse.getSuccess()) {
      log.warn("로그인 실패 (loginEx): {}", loginResponse.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(loginResponse);
    }

    // 4️⃣ 웹 클라이언트면 Refresh Token을 쿠키로 설정
    if (isWebClient) {
      log.info("웹 클라이언트 감지 → Refresh Token을 HTTP-only 쿠키로 설정");

      // Refresh Token을 HTTP-only 쿠키로 설정
      Cookie refreshTokenCookie = new Cookie("refreshToken", loginResponse.getRefreshToken());
      refreshTokenCookie.setHttpOnly(true);   // JavaScript 접근 불가 (XSS 방어)
      refreshTokenCookie.setSecure(false);     // HTTPS only (개발 환경에서는 false, 프로덕션에서는 true)
      refreshTokenCookie.setPath("/");        // 모든 경로에서 쿠키 전송
      refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7일 (초 단위)
      // refreshTokenCookie.setSameSite("Strict");  // CSRF 방어 (Spring Boot 3.x에서는 별도 설정 필요)

      response.addCookie(refreshTokenCookie);
      log.info("Refresh Token을 쿠키에 설정 완료");

      // 응답 바디에서 Refresh Token 제거 (쿠키로 전송했으므로)
      loginResponse.setRefreshToken(null);
      log.info("응답 바디에서 Refresh Token 제거 (보안 강화)");
    } else {
      // 5️⃣ 모바일 클라이언트면 Refresh Token을 JSON 응답에 포함
      log.info("모바일 클라이언트 감지 → Refresh Token을 JSON 응답에 포함");
      // loginResponse에 이미 refreshToken이 포함되어 있으므로 추가 작업 불필요
    }

    // 6️⃣ 로그인 성공 응답 반환
    log.info("로그인 성공 (loginEx): {}, 클라이언트: {}", loginRequest.getEmail(), clientType);
    return ResponseEntity.ok(loginResponse);
  }


  /**
   * Access Token 갱신 (하이브리드 방식 - 웹/모바일 구분)
   *
   * Refresh Token으로 새로운 Access Token을 발급받는다
   * 클라이언트 타입에 따라 Refresh Token을 다른 곳에서 읽는다:
   * - 웹 브라우저: HTTP-only 쿠키에서 Refresh Token 읽기
   * - 모바일 앱: 요청 바디에서 Refresh Token 읽기
   *
   * @param request HTTP 요청 (쿠키 읽기용)
   * @param body 요청 바디 (모바일용, Optional)
   * @return TokenRefreshResponse (새 Access Token)
   */
  @PostMapping("/refresh")
  public ResponseEntity<TokenRefreshResponse> refresh(
      HttpServletRequest request,
      @RequestBody(required = false) @Valid TokenRefreshRequest body
  ) {
    log.info("Access Token 갱신 요청");

    // 1️⃣ 클라이언트 타입 감지
    boolean isWebClient = ClientTypeDetector.isWebClient(request);
    String clientType = ClientTypeDetector.getClientTypeString(request);
    log.info("클라이언트 타입: {}", clientType);

    String refreshToken = null;

    // 2️⃣ 웹 클라이언트면 쿠키에서 Refresh Token 읽기
    if (isWebClient) {
      log.info("웹 클라이언트 → 쿠키에서 Refresh Token 읽기");

      Cookie[] cookies = request.getCookies();
      if (cookies != null) {
        for (Cookie cookie : cookies) {
          if ("refreshToken".equals(cookie.getName())) {
            refreshToken = cookie.getValue();
            log.debug("쿠키에서 Refresh Token 발견");
            break;
          }
        }
      }

      if (refreshToken == null) {
        log.warn("쿠키에 Refresh Token이 없음");
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(TokenRefreshResponse.error("Refresh Token이 없습니다. 다시 로그인해주세요."));
      }
    }
    // 3️⃣ 모바일 클라이언트면 요청 바디에서 Refresh Token 읽기
    else {
      log.info("모바일 클라이언트 → 요청 바디에서 Refresh Token 읽기");

      if (body == null || body.getRefreshToken() == null || body.getRefreshToken().isBlank()) {
        log.warn("요청 바디에 Refresh Token이 없음");
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(TokenRefreshResponse.error("Refresh Token은 필수입니다."));
      }

      refreshToken = body.getRefreshToken();
      log.debug("요청 바디에서 Refresh Token 발견");
    }

    // 4️⃣ Refresh Token으로 새 Access Token 발급
    TokenRefreshResponse response = authService.refreshAccessToken(refreshToken);

    // 5️⃣ 응답 반환
    if (response.getSuccess()) {
      log.info("Access Token 갱신 성공");
      return ResponseEntity.ok(response);
    } else {
      log.warn("Access Token 갱신 실패: {}", response.getMessage());
      return ResponseEntity
          .status(HttpStatus.UNAUTHORIZED)
          .body(response);
    }
  }
}
