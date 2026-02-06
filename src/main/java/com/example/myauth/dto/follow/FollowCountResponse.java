package com.example.myauth.dto.follow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 팔로우 카운트 응답 DTO
 * 팔로워/팔로잉 수 조회 시 반환
 *
 * 【응답 예시】
 * {
 *   "userId": 1,
 *   "followerCount": 100,
 *   "followingCount": 50
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowCountResponse {

  /**
   * 사용자 ID
   */
  private Long userId;

  /**
   * 팔로워 수 (나를 팔로우하는 사람 수)
   */
  private Long followerCount;

  /**
   * 팔로잉 수 (내가 팔로우하는 사람 수)
   */
  private Long followingCount;

  /**
   * 팔로우 카운트 응답 생성
   */
  public static FollowCountResponse of(Long userId, long followerCount, long followingCount) {
    return FollowCountResponse.builder()
        .userId(userId)
        .followerCount(followerCount)
        .followingCount(followingCount)
        .build();
  }
}
