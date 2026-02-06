package com.example.myauth.service;

import com.example.myauth.dto.follow.FollowCountResponse;
import com.example.myauth.dto.follow.FollowResponse;
import com.example.myauth.dto.follow.FollowUserResponse;
import com.example.myauth.entity.Follow;
import com.example.myauth.entity.User;
import com.example.myauth.exception.DuplicateFollowException;
import com.example.myauth.exception.FollowNotFoundException;
import com.example.myauth.exception.SelfFollowException;
import com.example.myauth.exception.UserNotFoundException;
import com.example.myauth.repository.FollowRepository;
import com.example.myauth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 팔로우 서비스
 * 사용자 간 팔로우 관계 관리 비즈니스 로직
 *
 * 【주요 기능】
 * - 팔로우/언팔로우
 * - 팔로워/팔로잉 목록 조회
 * - 팔로우 여부 확인
 * - 팔로워/팔로잉 수 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FollowService {

  private final FollowRepository followRepository;
  private final UserRepository userRepository;

  // ===== 팔로우/언팔로우 =====

  /**
   * 사용자 팔로우
   *
   * @param followerId 팔로우 하는 사람 ID (로그인 사용자)
   * @param followingId 팔로우 받는 사람 ID (대상 사용자)
   * @return 팔로우 응답
   */
  @Transactional
  public FollowResponse follow(Long followerId, Long followingId) {
    log.info("팔로우 요청 - followerId: {}, followingId: {}", followerId, followingId);

    // 1. 자기 자신 팔로우 확인
    if (followerId.equals(followingId)) {
      throw new SelfFollowException();
    }

    // 2. 대상 사용자 존재 확인
    User following = userRepository.findById(followingId)
        .orElseThrow(() -> new UserNotFoundException(followingId));

    // 3. 중복 팔로우 확인
    if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
      throw new DuplicateFollowException();
    }

    // 4. 팔로우 관계 생성
    User follower = userRepository.getReferenceById(followerId);
    Follow follow = Follow.create(follower, following);
    followRepository.save(follow);

    // 5. 팔로워/팔로잉 수 조회
    long followerCount = followRepository.countByFollowingId(followingId);
    long followingCount = followRepository.countByFollowerId(followingId);

    log.info("팔로우 완료 - followerId: {}, followingId: {}", followerId, followingId);

    return FollowResponse.follow(followingId, followerCount, followingCount);
  }

  /**
   * 사용자 언팔로우
   *
   * @param followerId 팔로우 취소하는 사람 ID (로그인 사용자)
   * @param followingId 팔로우 취소 대상 ID
   * @return 언팔로우 응답
   */
  @Transactional
  public FollowResponse unfollow(Long followerId, Long followingId) {
    log.info("언팔로우 요청 - followerId: {}, followingId: {}", followerId, followingId);

    // 1. 팔로우 관계 존재 확인
    Follow follow = followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
        .orElseThrow(() -> new FollowNotFoundException("팔로우하지 않은 사용자입니다."));

    // 2. 팔로우 관계 삭제
    followRepository.delete(follow);

    // 3. 팔로워/팔로잉 수 조회
    long followerCount = followRepository.countByFollowingId(followingId);
    long followingCount = followRepository.countByFollowerId(followingId);

    log.info("언팔로우 완료 - followerId: {}, followingId: {}", followerId, followingId);

    return FollowResponse.unfollow(followingId, followerCount, followingCount);
  }

  // ===== 팔로워 목록 조회 =====

  /**
   * 팔로워 목록 조회 (나를 팔로우하는 사람들)
   *
   * @param currentUserId 로그인 사용자 ID (팔로우 여부 확인용)
   * @param userId 조회할 사용자 ID
   * @param pageable 페이지 정보
   * @return 팔로워 목록 페이지
   */
  @Transactional(readOnly = true)
  public Page<FollowUserResponse> getFollowers(Long currentUserId, Long userId, Pageable pageable) {
    log.info("팔로워 목록 조회 - userId: {}, page: {}", userId, pageable.getPageNumber());

    // 1. 사용자 존재 확인
    if (!userRepository.existsById(userId)) {
      throw new UserNotFoundException(userId);
    }

    // 2. 팔로워 목록 조회
    Page<Follow> follows = followRepository.findFollowsByFollowingId(userId, pageable);

    // 3. 현재 사용자가 팔로우하는 사용자 ID 목록 조회
    List<Long> followerIds = follows.getContent().stream()
        .map(f -> f.getFollower().getId())
        .toList();

    List<Long> followingIds = currentUserId != null && !followerIds.isEmpty()
        ? followRepository.findFollowingIdsByFollowerId(currentUserId, followerIds)
        : Collections.emptyList();

    // 4. 응답 DTO 변환
    return follows.map(follow -> FollowUserResponse.from(
        follow.getFollower(),
        followingIds.contains(follow.getFollower().getId()),
        follow.getCreatedAt()
    ));
  }

  // ===== 팔로잉 목록 조회 =====

  /**
   * 팔로잉 목록 조회 (내가 팔로우하는 사람들)
   *
   * @param currentUserId 로그인 사용자 ID (팔로우 여부 확인용)
   * @param userId 조회할 사용자 ID
   * @param pageable 페이지 정보
   * @return 팔로잉 목록 페이지
   */
  @Transactional(readOnly = true)
  public Page<FollowUserResponse> getFollowings(Long currentUserId, Long userId, Pageable pageable) {
    log.info("팔로잉 목록 조회 - userId: {}, page: {}", userId, pageable.getPageNumber());

    // 1. 사용자 존재 확인
    if (!userRepository.existsById(userId)) {
      throw new UserNotFoundException(userId);
    }

    // 2. 팔로잉 목록 조회
    Page<Follow> follows = followRepository.findFollowsByFollowerId(userId, pageable);

    // 3. 현재 사용자가 팔로우하는 사용자 ID 목록 조회
    List<Long> followingIds = follows.getContent().stream()
        .map(f -> f.getFollowing().getId())
        .toList();

    List<Long> currentUserFollowingIds = currentUserId != null && !followingIds.isEmpty()
        ? followRepository.findFollowingIdsByFollowerId(currentUserId, followingIds)
        : Collections.emptyList();

    // 4. 응답 DTO 변환
    return follows.map(follow -> FollowUserResponse.from(
        follow.getFollowing(),
        currentUserFollowingIds.contains(follow.getFollowing().getId()),
        follow.getCreatedAt()
    ));
  }

  // ===== 팔로우 여부 확인 =====

  /**
   * 팔로우 여부 확인
   *
   * @param followerId 팔로우 하는 사람 ID
   * @param followingId 팔로우 받는 사람 ID
   * @return 팔로우 여부
   */
  @Transactional(readOnly = true)
  public boolean isFollowing(Long followerId, Long followingId) {
    return followRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
  }

  // ===== 팔로우 카운트 =====

  /**
   * 팔로워/팔로잉 수 조회
   *
   * @param userId 사용자 ID
   * @return 팔로우 카운트 응답
   */
  @Transactional(readOnly = true)
  public FollowCountResponse getFollowCounts(Long userId) {
    log.info("팔로우 카운트 조회 - userId: {}", userId);

    // 사용자 존재 확인
    if (!userRepository.existsById(userId)) {
      throw new UserNotFoundException(userId);
    }

    long followerCount = followRepository.countByFollowingId(userId);
    long followingCount = followRepository.countByFollowerId(userId);

    return FollowCountResponse.of(userId, followerCount, followingCount);
  }
}
