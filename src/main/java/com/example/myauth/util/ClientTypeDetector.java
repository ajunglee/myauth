package com.example.myauth.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 클라이언트 타입(웹/모바일)을 감지하는 유틸리티 클래스
 *
 * 요청 헤더를 분석하여 웹 브라우저인지 모바일 네이티브 앱인지 구분한다
 * 이를 통해 토큰 전송 방식을 다르게 처리할 수 있다
 * - 웹: Refresh Token을 HTTP-only 쿠키로 전송
 * - 모바일: 모든 토큰을 JSON 응답 바디로 전송
 */
@Slf4j
public class ClientTypeDetector {

  /**
   * 클라이언트가 웹 브라우저인지 판단
   *
   * 판단 기준:
   * 1. 커스텀 헤더 'X-Client-Type'이 'mobile-app'이면 → 모바일
   * 2. User-Agent에 네이티브 앱 특유의 키워드가 있으면 → 모바일
   * 3. 그 외는 모두 웹 브라우저로 간주
   *
   * @param request HTTP 요청 객체
   * @return true면 웹 브라우저, false면 모바일 앱
   */
  public static boolean isWebClient(HttpServletRequest request) {
    // 1️⃣ 커스텀 헤더로 판단 (가장 명확하고 권장되는 방법)
    String clientType = request.getHeader("X-Client-Type");
    if ("mobile-app".equalsIgnoreCase(clientType)) {
      log.debug("모바일 앱 감지 (X-Client-Type 헤더)");
      return false;
    }
    if ("web".equalsIgnoreCase(clientType)) {
      log.debug("웹 클라이언트 감지 (X-Client-Type 헤더)");
      return true;
    }

    // 2️⃣ User-Agent로 판단 (보조 수단)
    String userAgent = request.getHeader("User-Agent");
    if (userAgent != null) {
      userAgent = userAgent.toLowerCase();

      // 모바일 네이티브 앱의 특징적인 User-Agent 패턴
      if (isMobileApp(userAgent)) {
        log.debug("모바일 앱 감지 (User-Agent): {}", userAgent);
        return false;
      }
    }

    // 3️⃣ 기본값: 웹 클라이언트로 간주
    log.debug("웹 클라이언트로 간주 (기본값)");
    return true;
  }

  /**
   * User-Agent를 분석하여 모바일 네이티브 앱인지 판단
   *
   * 모바일 앱의 특징:
   * - 커스텀 앱 이름이 포함됨 (예: MyAuthMobileApp, MyApp)
   * - Android 네이티브 HTTP 클라이언트: okhttp, volley
   * - iOS 네이티브 HTTP 클라이언트: CFNetwork, NSURLConnection
   * - React Native, Flutter 등의 프레임워크
   *
   * @param userAgent User-Agent 문자열 (소문자로 변환된 상태)
   * @return true면 모바일 네이티브 앱
   */
  private static boolean isMobileApp(String userAgent) {
    // 커스텀 앱 이름 (실제 앱 이름으로 변경 필요)
    if (userAgent.contains("myauthmobileapp") || userAgent.contains("myapp")) {
      return true;
    }

    // Android 네이티브 HTTP 클라이언트
    if (userAgent.contains("okhttp") ||      // OkHttp (가장 많이 사용)
        userAgent.contains("volley") ||      // Volley
        userAgent.contains("retrofit")) {    // Retrofit (내부적으로 OkHttp 사용)
      return true;
    }

    // iOS 네이티브 HTTP 클라이언트
    if (userAgent.contains("cfnetwork") ||         // CFNetwork (iOS 기본)
        userAgent.contains("nsurlsession") ||      // NSURLSession
        userAgent.contains("nsurlconnection")) {   // NSURLConnection (구버전)
      return true;
    }

    // React Native
    if (userAgent.contains("react native") ||
        userAgent.contains("reactnative")) {
      return true;
    }

    // Flutter
    if (userAgent.contains("flutter") ||
        userAgent.contains("dart")) {
      return true;
    }

    // Expo (React Native 기반)
    if (userAgent.contains("expo")) {
      return true;
    }

    return false;
  }

  /**
   * 클라이언트 타입을 문자열로 반환 (디버깅용)
   *
   * @param request HTTP 요청 객체
   * @return "WEB" 또는 "MOBILE"
   */
  public static String getClientTypeString(HttpServletRequest request) {
    return isWebClient(request) ? "WEB" : "MOBILE";
  }

  /**
   * User-Agent 전체를 로깅 (디버깅용)
   *
   * @param request HTTP 요청 객체
   */
  public static void logUserAgent(HttpServletRequest request) {
    String userAgent = request.getHeader("User-Agent");
    String clientType = request.getHeader("X-Client-Type");
    log.info("=== 클라이언트 정보 ===");
    log.info("X-Client-Type: {}", clientType);
    log.info("User-Agent: {}", userAgent);
    log.info("판단 결과: {}", getClientTypeString(request));
    log.info("======================");
  }
}
