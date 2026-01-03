package com.example.myauth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 프로필 수정 응답 DTO
 * User 정보와 UserProfile 정보를 함께 반환
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateResponse {

  // ===== User 정보 =====

  /**
   * 사용자 ID
   */
  private Long userId;

  /**
   * 이메일
   */
  private String email;

  /**
   * 닉네임 (User.name)
   */
  private String name;

  /**
   * 프로필 이미지 URL
   */
  private String profileImage;

  /**
   * OAuth 제공자 (KAKAO, GOOGLE 등)
   */
  private String provider;

  // ===== UserProfile 정보 =====

  /**
   * UserProfile ID
   */
  private Long profileId;

  /**
   * 성
   */
  private String lastName;

  /**
   * 이름
   */
  private String firstName;

  /**
   * 전화번호
   */
  private String phoneNumber;

  /**
   * 국가 코드
   */
  private Long country;

  /**
   * 주소 1
   */
  private String address1;

  /**
   * 주소 2
   */
  private String address2;

  /**
   * 생년월일
   */
  private LocalDateTime birth;

  /**
   * 배경 이미지 URL
   */
  private String bgImage;

  /**
   * 프로필 생성 일시
   */
  private LocalDateTime createdAt;

  /**
   * 프로필 수정 일시
   */
  private LocalDateTime updatedAt;
}
