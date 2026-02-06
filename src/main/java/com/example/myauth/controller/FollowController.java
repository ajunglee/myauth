package com.example.myauth.controller;

import com.example.myauth.dto.ApiResponse;
import com.example.myauth.dto.follow.FollowCountResponse;
import com.example.myauth.dto.follow.FollowResponse;
import com.example.myauth.dto.follow.FollowUserResponse;
import com.example.myauth.entity.User;
import com.example.myauth.service.FollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 팔로우 컨트롤러
 * 사용자 팔로우 관련 API 엔드포인트 제공
 *
 * 【API 목록】
 * - POST   /api/users/{userId}/follow      : 팔로우
 * - DELETE /api/users/{userId}/follow      : 언팔로우
 * - GET    /api/users/{userId}/followers   : 팔로워 목록
 * - GET    /api/users/{userId}/followings  : 팔로잉 목록
 * - GET    /api/users/{userId}/follow/count: 팔로워/팔로잉 수
 * - GET    /api/users/{userId}/follow/check: 팔로우 여부 확인
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class FollowController {

  private final FollowService followService;

  // ===== 팔로우/언팔로우 =====

  /**
   * 사용자 팔로우
   *
   * POST /api/users/{userId}/follow
   *
   * 【응답 예시】
   * {
   *   "success": true,
   *   "message": "팔로우 완료",
   *   "data": {
   *     "userId": 1,
   *     "following": true,
   *     "followerCount": 100,
   *     "followingCount": 50
   *   }
   * }
   */
  @PostMapping("/{userId}/follow")
  public ResponseEntity<ApiResponse<FollowResponse>> follow(
      @AuthenticationPrincipal User user,
      @PathVariable Long userId
  ) {
    log.info("팔로우 요청 - followerId: {}, followingId: {}", user.getId(), userId);

    FollowResponse response = followService.follow(user.getId(), userId);

    return ResponseEntity.ok(ApiResponse.success("팔로우 완료", response));
  }

  /**
   * 사용자 언팔로우
   *
   * DELETE /api/users/{userId}/follow
   */
  @DeleteMapping("/{userId}/follow")
  public ResponseEntity<ApiResponse<FollowResponse>> unfollow(
      @AuthenticationPrincipal User user,
      @PathVariable Long userId
  ) {
    log.info("언팔로우 요청 - followerId: {}, followingId: {}", user.getId(), userId);

    FollowResponse response = followService.unfollow(user.getId(), userId);

    return ResponseEntity.ok(ApiResponse.success("언팔로우 완료", response));
  }

  // ===== 팔로워/팔로잉 목록 조회 =====

  /**
   * 팔로워 목록 조회 (특정 사용자를 팔로우하는 사람들)
   *
   * GET /api/users/{userId}/followers?page=0&size=20
   *
   * 【쿼리 파라미터】
   * - page: 페이지 번호 (0부터 시작, 기본값 0)
   * - size: 페이지 크기 (기본값 20, 최대 50)
   */
  @GetMapping("/{userId}/followers")
  public ResponseEntity<ApiResponse<Page<FollowUserResponse>>> getFollowers(
      @AuthenticationPrincipal User user,
      @PathVariable Long userId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    log.info("팔로워 목록 조회 - userId: {}", userId);

    // 페이지 크기 제한
    if (size > 50) size = 50;

    Pageable pageable = PageRequest.of(page, size);
    Long currentUserId = user != null ? user.getId() : null;

    Page<FollowUserResponse> followers = followService.getFollowers(currentUserId, userId, pageable);

    return ResponseEntity.ok(ApiResponse.success("팔로워 목록 조회 성공", followers));
  }

  /**
   * 팔로잉 목록 조회 (특정 사용자가 팔로우하는 사람들)
   *
   * GET /api/users/{userId}/followings?page=0&size=20
   */
  @GetMapping("/{userId}/followings")
  public ResponseEntity<ApiResponse<Page<FollowUserResponse>>> getFollowings(
      @AuthenticationPrincipal User user,
      @PathVariable Long userId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    log.info("팔로잉 목록 조회 - userId: {}", userId);

    // 페이지 크기 제한
    if (size > 50) size = 50;

    Pageable pageable = PageRequest.of(page, size);
    Long currentUserId = user != null ? user.getId() : null;

    Page<FollowUserResponse> followings = followService.getFollowings(currentUserId, userId, pageable);

    return ResponseEntity.ok(ApiResponse.success("팔로잉 목록 조회 성공", followings));
  }

  // ===== 팔로우 카운트 조회 =====

  /**
   * 팔로워/팔로잉 수 조회
   *
   * GET /api/users/{userId}/follow/count
   *
   * 【응답 예시】
   * {
   *   "userId": 1,
   *   "followerCount": 100,
   *   "followingCount": 50
   * }
   */
  @GetMapping("/{userId}/follow/count")
  public ResponseEntity<ApiResponse<FollowCountResponse>> getFollowCounts(
      @PathVariable Long userId
  ) {
    log.info("팔로우 카운트 조회 - userId: {}", userId);

    FollowCountResponse response = followService.getFollowCounts(userId);

    return ResponseEntity.ok(ApiResponse.success("팔로우 카운트 조회 성공", response));
  }

  // ===== 팔로우 여부 확인 =====

  /**
   * 팔로우 여부 확인
   *
   * GET /api/users/{userId}/follow/check
   *
   * 【응답 예시】
   * {
   *   "following": true
   * }
   */
  @GetMapping("/{userId}/follow/check")
  public ResponseEntity<ApiResponse<Boolean>> checkFollowing(
      @AuthenticationPrincipal User user,
      @PathVariable Long userId
  ) {
    log.info("팔로우 여부 확인 - followerId: {}, followingId: {}", user.getId(), userId);

    boolean isFollowing = followService.isFollowing(user.getId(), userId);

    return ResponseEntity.ok(ApiResponse.success("팔로우 여부 확인 성공", isFollowing));
  }
}
