package com.example.myauth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token Refresh 응답 DTO
 *
 * Access Token 갱신 성공/실패 응답
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenRefreshResponse {

  /**
   * 성공 여부
   */
  private Boolean success;

  /**
   * 응답 메시지
   */
  private String message;

  /**
   * 새로 발급된 Access Token
   * 성공 시에만 포함됨
   */
  private String accessToken;

  /**
   * 새로 발급된 Refresh Token (선택적)
   * Refresh Token Rotation을 사용하는 경우에만 포함됨
   * 현재는 사용하지 않으므로 null
   */
  private String refreshToken;

  /**
   * 성공 응답 생성 헬퍼 메서드
   *
   * @param accessToken 새로 발급된 Access Token
   * @return 성공 응답
   */
  public static TokenRefreshResponse success(String accessToken) {
    return TokenRefreshResponse.builder()
        .success(true)
        .message("Access Token이 갱신되었습니다")
        .accessToken(accessToken)
        .refreshToken(null)  // Refresh Token Rotation 미사용
        .build();
  }

  /**
   * 실패 응답 생성 헬퍼 메서드
   *
   * @param message 에러 메시지
   * @return 실패 응답
   */
  public static TokenRefreshResponse error(String message) {
    return TokenRefreshResponse.builder()
        .success(false)
        .message(message)
        .accessToken(null)
        .refreshToken(null)
        .build();
  }
}
