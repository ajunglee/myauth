package com.example.myauth.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS (Cross-Origin Resource Sharing) 설정
 * 프론트엔드와 백엔드가 다른 출처(도메인, 포트)에 있을 때 필요
 *
 * 주요 설정:
 * - allowedOrigins: 요청을 허용할 프론트엔드 출처
 * - allowedMethods: 허용할 HTTP 메서드
 * - allowedHeaders: 허용할 요청 헤더
 * - allowCredentials: 쿠키 전송 허용 여부
 * - maxAge: preflight 요청 캐시 시간
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CorsConfig implements WebMvcConfigurer {

  private final AppProperties appProperties;

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    log.info("CORS 설정 적용 중...");
    log.info("허용 출처: {}", appProperties.getCors().getAllowedOrigins());
    log.info("허용 메서드: {}", appProperties.getCors().getAllowedMethods());
    log.info("쿠키 전송: {}", appProperties.getCors().isAllowCredentials());

    registry.addMapping("/**")  // 모든 경로에 대해 CORS 설정 적용
        // 허용할 출처 (프론트엔드 URL)
        .allowedOriginPatterns(appProperties.getCors().getAllowedOrigins().toArray(new String[0]))

        // 허용할 HTTP 메서드
        .allowedMethods(appProperties.getCors().getAllowedMethods().toArray(new String[0]))

        // 허용할 요청 헤더
        .allowedHeaders("*")  // 모든 헤더 허용

        // 쿠키 전송 허용 여부 (Refresh Token을 쿠키로 전송하므로 true)
        .allowCredentials(appProperties.getCors().isAllowCredentials())

        // preflight 요청 캐시 시간 (1시간)
        .maxAge(3600);

    log.info("CORS 설정 완료");
  }
}
