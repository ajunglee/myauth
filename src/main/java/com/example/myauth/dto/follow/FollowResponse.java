package com.example.myauth.dto.follow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 팔로우/언팔로우 응답 DTO
 * 팔로우 상태 변경 후 반환
 *
 * 【응답 예시】
 * {
 *   "userId": 1,
 *   "following": true,
 *   "followerCount": 100,
 *   "followingCount": 50
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowResponse {

  /**
   * 대상 사용자 ID
   */
  private Long userId;

  /**
   * 팔로우 상태
   * - true: 팔로우 중
   * - false: 팔로우 해제됨
   */
  private Boolean following;

  /**
   * 대상 사용자의 팔로워 수
   */
  private Long followerCount;

  /**
   * 대상 사용자의 팔로잉 수
   */
  private Long followingCount;

  /**
   * 팔로우 응답 생성
   */
  public static FollowResponse follow(Long userId, long followerCount, long followingCount) {
    return FollowResponse.builder()
        .userId(userId)
        .following(true)
        .followerCount(followerCount)
        .followingCount(followingCount)
        .build();
  }

  /**
   * 언팔로우 응답 생성
   */
  public static FollowResponse unfollow(Long userId, long followerCount, long followingCount) {
    return FollowResponse.builder()
        .userId(userId)
        .following(false)
        .followerCount(followerCount)
        .followingCount(followingCount)
        .build();
  }
}
