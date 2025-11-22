package com.example.myauth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Refresh Token으로 Access Token 갱신 요청 DTO
 *
 * 모바일 클라이언트용:
 * - 모바일 앱은 Refresh Token을 요청 바디에 포함하여 전송
 * - 웹 클라이언트는 쿠키로 전송하므로 이 DTO를 사용하지 않음 (Optional)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshRequest {

  /**
   * Refresh Token
   * 모바일 클라이언트만 사용 (웹은 쿠키에서 자동으로 읽음)
   */
  @NotBlank(message = "Refresh Token은 필수입니다")
  private String refreshToken;
}
