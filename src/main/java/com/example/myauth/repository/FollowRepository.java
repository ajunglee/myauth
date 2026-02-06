package com.example.myauth.repository;

import com.example.myauth.entity.Follow;
import com.example.myauth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 팔로우 리포지토리
 * 사용자 간 팔로우 관계 관리
 *
 * 【용어 정리】
 * - follower: 팔로우 하는 사람 (주체) - "A가 B를 팔로우" 에서 A
 * - following: 팔로우 받는 사람 (대상) - "A가 B를 팔로우" 에서 B
 * - followers: 나를 팔로우하는 사람들 (팔로워 목록)
 * - followings: 내가 팔로우하는 사람들 (팔로잉 목록)
 *
 * 【주요 기능】
 * - 팔로우 추가/삭제
 * - 팔로우 여부 확인
 * - 팔로워/팔로잉 목록 조회
 * - 팔로워/팔로잉 수 카운팅
 */
@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

  // ===== 팔로우 존재 여부 확인 =====

  /**
   * 팔로우 여부 확인
   *
   * @param followerId 팔로우 하는 사람 ID
   * @param followingId 팔로우 받는 사람 ID
   * @return 팔로우 여부
   */
  boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

  // ===== 팔로우 조회 =====

  /**
   * 팔로우 관계 조회
   *
   * @param followerId 팔로우 하는 사람 ID
   * @param followingId 팔로우 받는 사람 ID
   * @return 팔로우 Optional
   */
  Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

  // ===== 팔로우 삭제 =====

  /**
   * 팔로우 관계 삭제
   *
   * @param followerId 팔로우 하는 사람 ID
   * @param followingId 팔로우 받는 사람 ID
   */
  void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);

  // ===== 팔로워 목록 조회 (나를 팔로우하는 사람들) =====

  /**
   * 팔로워 목록 조회 (User 엔티티 반환)
   * 특정 사용자를 팔로우하는 사람들 목록
   *
   * @param userId 대상 사용자 ID
   * @param pageable 페이지 정보
   * @return 팔로워 목록 페이지
   */
  @Query("SELECT f.follower FROM Follow f " +
      "WHERE f.following.id = :userId " +
      "ORDER BY f.createdAt DESC")
  Page<User> findFollowersByUserId(@Param("userId") Long userId, Pageable pageable);

  /**
   * 팔로워 목록 조회 (Follow 엔티티 반환, 팔로우 일시 포함)
   *
   * @param userId 대상 사용자 ID
   * @param pageable 페이지 정보
   * @return 팔로우 관계 페이지
   */
  @Query("SELECT f FROM Follow f " +
      "JOIN FETCH f.follower " +
      "WHERE f.following.id = :userId " +
      "ORDER BY f.createdAt DESC")
  Page<Follow> findFollowsByFollowingId(@Param("userId") Long userId, Pageable pageable);

  // ===== 팔로잉 목록 조회 (내가 팔로우하는 사람들) =====

  /**
   * 팔로잉 목록 조회 (User 엔티티 반환)
   * 특정 사용자가 팔로우하는 사람들 목록
   *
   * @param userId 대상 사용자 ID
   * @param pageable 페이지 정보
   * @return 팔로잉 목록 페이지
   */
  @Query("SELECT f.following FROM Follow f " +
      "WHERE f.follower.id = :userId " +
      "ORDER BY f.createdAt DESC")
  Page<User> findFollowingsByUserId(@Param("userId") Long userId, Pageable pageable);

  /**
   * 팔로잉 목록 조회 (Follow 엔티티 반환, 팔로우 일시 포함)
   *
   * @param userId 대상 사용자 ID
   * @param pageable 페이지 정보
   * @return 팔로우 관계 페이지
   */
  @Query("SELECT f FROM Follow f " +
      "JOIN FETCH f.following " +
      "WHERE f.follower.id = :userId " +
      "ORDER BY f.createdAt DESC")
  Page<Follow> findFollowsByFollowerId(@Param("userId") Long userId, Pageable pageable);

  // ===== 카운트 =====

  /**
   * 팔로워 수 조회 (나를 팔로우하는 사람 수)
   *
   * @param userId 대상 사용자 ID
   * @return 팔로워 수
   */
  long countByFollowingId(Long userId);

  /**
   * 팔로잉 수 조회 (내가 팔로우하는 사람 수)
   *
   * @param userId 대상 사용자 ID
   * @return 팔로잉 수
   */
  long countByFollowerId(Long userId);

  // ===== 배치 조회 =====

  /**
   * 특정 사용자가 팔로우하는 사람 ID 목록 조회
   * 팔로우 여부 일괄 확인용
   *
   * @param followerId 팔로우 하는 사람 ID
   * @param followingIds 확인할 사용자 ID 목록
   * @return 팔로우 중인 사용자 ID 목록
   */
  @Query("SELECT f.following.id FROM Follow f " +
      "WHERE f.follower.id = :followerId " +
      "AND f.following.id IN :followingIds")
  List<Long> findFollowingIdsByFollowerId(
      @Param("followerId") Long followerId,
      @Param("followingIds") List<Long> followingIds);

  // ===== 맞팔로우 확인 =====

  /**
   * 상호 팔로우(맞팔) 여부 확인
   * A가 B를 팔로우하고, B도 A를 팔로우하는지 확인
   *
   * @param userId1 사용자1 ID
   * @param userId2 사용자2 ID
   * @return 맞팔로우 여부
   */
  @Query("SELECT COUNT(f) = 2 FROM Follow f " +
      "WHERE (f.follower.id = :userId1 AND f.following.id = :userId2) " +
      "OR (f.follower.id = :userId2 AND f.following.id = :userId1)")
  boolean isMutualFollow(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}
