package com.example.myauth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Refresh Token으로 Access Token 갱신 요청 DTO
 *
 * 클라이언트별 사용 방식:
 * - 모바일 앱: Refresh Token을 요청 바디에 포함하여 전송 (이 DTO 사용)
 * - 웹 브라우저: Refresh Token을 HTTP-only 쿠키로 전송 (이 DTO는 비어있거나 null)
 *
 * NOTE: @NotBlank validation을 제거한 이유
 * - 웹 클라이언트는 쿠키로 토큰을 전송하므로 body가 비어있어도 정상
 * - AuthController의 refresh 메서드에서 클라이언트 타입별로 validation 수행
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshRequest {

  /**
   * Refresh Token
   * - 모바일 클라이언트: 필수 (요청 바디에 포함)
   * - 웹 클라이언트: Optional (쿠키에서 자동으로 읽음)
   */
  private String refreshToken;
}
