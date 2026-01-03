package com.example.myauth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 프로필 수정 요청 DTO
 * User 테이블과 UserProfile 테이블의 정보를 동시에 수정
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {

  // ===== User 테이블 필드 =====

  /**
   * 닉네임 (User.name)
   */
  private String name;

  /**
   * 프로필 이미지 URL (User.profileImage)
   */
  private String profileImage;

  // ===== UserProfile 테이블 필드 =====

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
}
