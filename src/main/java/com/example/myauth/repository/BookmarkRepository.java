package com.example.myauth.repository;

import com.example.myauth.entity.Bookmark;
import com.example.myauth.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 북마크 리포지토리
 * 사용자의 게시글 북마크(저장) 관리
 *
 * 【주요 기능】
 * - 북마크 추가/삭제
 * - 북마크 여부 확인
 * - 북마크한 게시글 목록 조회
 */
@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

  // ===== 북마크 존재 여부 확인 =====

  /**
   * 북마크 여부 확인
   *
   * @param userId 사용자 ID
   * @param postId 게시글 ID
   * @return 북마크 여부
   */
  boolean existsByUserIdAndPostId(Long userId, Long postId);

  // ===== 북마크 조회 =====

  /**
   * 북마크 조회
   *
   * @param userId 사용자 ID
   * @param postId 게시글 ID
   * @return 북마크 Optional
   */
  Optional<Bookmark> findByUserIdAndPostId(Long userId, Long postId);

  // ===== 북마크 삭제 =====

  /**
   * 북마크 삭제
   *
   * @param userId 사용자 ID
   * @param postId 게시글 ID
   */
  void deleteByUserIdAndPostId(Long userId, Long postId);

  // ===== 북마크한 게시글 목록 조회 =====

  /**
   * 사용자가 북마크한 게시글 목록 조회 (삭제되지 않은 게시글만)
   *
   * @param userId 사용자 ID
   * @param pageable 페이지 정보
   * @return 북마크한 게시글 페이지
   */
  @Query("SELECT b.post FROM Bookmark b " +
      "WHERE b.user.id = :userId " +
      "AND b.post.isDeleted = false " +
      "ORDER BY b.createdAt DESC")
  Page<Post> findBookmarkedPostsByUserId(@Param("userId") Long userId, Pageable pageable);

  /**
   * 사용자가 북마크한 게시글 목록 조회 (북마크 정보 포함)
   *
   * @param userId 사용자 ID
   * @param pageable 페이지 정보
   * @return 북마크 페이지
   */
  @Query("SELECT b FROM Bookmark b " +
      "JOIN FETCH b.post p " +
      "JOIN FETCH p.user " +
      "WHERE b.user.id = :userId " +
      "AND p.isDeleted = false " +
      "ORDER BY b.createdAt DESC")
  Page<Bookmark> findByUserIdWithPost(@Param("userId") Long userId, Pageable pageable);

  // ===== 카운트 =====

  /**
   * 사용자의 북마크 수 조회
   *
   * @param userId 사용자 ID
   * @return 북마크 수
   */
  long countByUserId(Long userId);

  /**
   * 게시글의 북마크 수 조회
   *
   * @param postId 게시글 ID
   * @return 북마크 수
   */
  long countByPostId(Long postId);

  // ===== 배치 조회 =====

  /**
   * 특정 사용자가 북마크한 게시글 ID 목록 조회
   * 북마크 여부 일괄 확인용
   *
   * @param userId 사용자 ID
   * @param postIds 확인할 게시글 ID 목록
   * @return 북마크한 게시글 ID 목록
   */
  @Query("SELECT b.post.id FROM Bookmark b " +
      "WHERE b.user.id = :userId " +
      "AND b.post.id IN :postIds")
  List<Long> findBookmarkedPostIdsByUserId(
      @Param("userId") Long userId,
      @Param("postIds") List<Long> postIds);
}
