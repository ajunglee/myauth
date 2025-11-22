package com.example.myauth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JWT 인증 오류 응답 DTO
 * 클라이언트가 토큰 만료와 유효하지 않은 토큰을 구분할 수 있도록 상세한 에러 정보 제공
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtErrorResponse {

  /**
   * 에러 코드
   * - TOKEN_EXPIRED: 토큰이 만료됨 (클라이언트는 /refresh 호출해야 함)
   * - INVALID_TOKEN: 유효하지 않은 토큰 (클라이언트는 재로그인 필요)
   * - NO_TOKEN: 토큰이 없음 (클라이언트는 로그인 필요)
   */
  private String errorCode;

  /**
   * 사용자에게 표시할 에러 메시지
   */
  private String message;

  /**
   * 클라이언트가 취해야 할 액션
   * - REFRESH_TOKEN: /refresh 엔드포인트 호출
   * - LOGIN_REQUIRED: 재로그인 필요
   */
  private String action;

  /**
   * 요청 경로 (디버깅용)
   */
  private String path;

  /**
   * 토큰 만료 오류 응답 생성
   */
  public static JwtErrorResponse tokenExpired(String path) {
    return JwtErrorResponse.builder()
        .errorCode("TOKEN_EXPIRED")
        .message("Access Token이 만료되었습니다. 토큰을 갱신해주세요.")
        .action("REFRESH_TOKEN")
        .path(path)
        .build();
  }

  /**
   * 유효하지 않은 토큰 오류 응답 생성
   */
  public static JwtErrorResponse invalidToken(String path) {
    return JwtErrorResponse.builder()
        .errorCode("INVALID_TOKEN")
        .message("유효하지 않은 토큰입니다. 다시 로그인해주세요.")
        .action("LOGIN_REQUIRED")
        .path(path)
        .build();
  }

  /**
   * 토큰 없음 오류 응답 생성
   */
  public static JwtErrorResponse noToken(String path) {
    return JwtErrorResponse.builder()
        .errorCode("NO_TOKEN")
        .message("인증이 필요합니다. 로그인해주세요.")
        .action("LOGIN_REQUIRED")
        .path(path)
        .build();
  }
}
