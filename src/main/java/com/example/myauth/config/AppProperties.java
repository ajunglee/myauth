package com.example.myauth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 애플리케이션 커스텀 설정
 * application.yaml의 app.* 설정을 매핑
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

  /**
   * 쿠키 보안 설정
   */
  private Cookie cookie = new Cookie();

  /**
   * CORS 설정
   */
  private Cors cors = new Cors();

  @Getter
  @Setter
  public static class Cookie {
    /**
     * 쿠키 Secure 플래그
     * - true: HTTPS에서만 쿠키 전송 (프로덕션)
     * - false: HTTP에서도 쿠키 전송 허용 (개발)
     */
    private boolean secure = false;  // 기본값: false (개발 환경)
  }

  @Getter
  @Setter
  public static class Cors {
    /**
     * CORS 허용 출처 목록
     */
    private List<String> allowedOrigins = List.of("http://localhost:3000");

    /**
     * CORS 허용 HTTP 메서드
     */
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");

    /**
     * 쿠키 전송 허용 여부
     */
    private boolean allowCredentials = true;
  }
}
