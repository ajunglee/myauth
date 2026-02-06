package com.example.myauth.dto.follow;

import com.example.myauth.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 팔로우 사용자 정보 응답 DTO
 * 팔로워/팔로잉 목록에서 사용자 정보 표시용
 *
 * 【응답 예시】
 * {
 *   "id": 1,
 *   "name": "홍길동",
 *   "email": "hong@example.com",
 *   "profileImage": "http://...",
 *   "isFollowing": true,
 *   "followedAt": "2025-01-24T10:30:00"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowUserResponse {

  /**
   * 사용자 ID
   */
  private Long id;

  /**
   * 사용자 이름
   */
  private String name;

  /**
   * 이메일 (선택적 표시)
   */
  private String email;

  /**
   * 프로필 이미지 URL
   */
  private String profileImage;

  /**
   * 현재 로그인 사용자가 이 사용자를 팔로우하는지 여부
   */
  private Boolean isFollowing;

  /**
   * 팔로우한 일시
   */
  private LocalDateTime followedAt;

  /**
   * User 엔티티 → DTO 변환 (기본)
   */
  public static FollowUserResponse from(User user) {
    return FollowUserResponse.builder()
        .id(user.getId())
        .name(user.getName())
        .email(user.getEmail())
        .profileImage(user.getProfileImage())
        .build();
  }

  /**
   * User 엔티티 → DTO 변환 (팔로우 여부 및 일시 포함)
   */
  public static FollowUserResponse from(User user, boolean isFollowing, LocalDateTime followedAt) {
    return FollowUserResponse.builder()
        .id(user.getId())
        .name(user.getName())
        .email(user.getEmail())
        .profileImage(user.getProfileImage())
        .isFollowing(isFollowing)
        .followedAt(followedAt)
        .build();
  }
}
