package com.example.myauth.service;

import com.example.myauth.dto.bookmark.BookmarkResponse;
import com.example.myauth.dto.bookmark.BookmarkedPostResponse;
import com.example.myauth.entity.Bookmark;
import com.example.myauth.entity.Post;
import com.example.myauth.entity.User;
import com.example.myauth.exception.BookmarkNotFoundException;
import com.example.myauth.exception.DuplicateBookmarkException;
import com.example.myauth.exception.PostNotFoundException;
import com.example.myauth.repository.BookmarkRepository;
import com.example.myauth.repository.PostRepository;
import com.example.myauth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 북마크 서비스
 * 게시글 북마크(저장) 관리 비즈니스 로직
 *
 * 【주요 기능】
 * - 게시글 북마크 추가/삭제
 * - 북마크한 게시글 목록 조회
 * - 북마크 여부 확인
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookmarkService {

  private final BookmarkRepository bookmarkRepository;
  private final PostRepository postRepository;
  private final UserRepository userRepository;

  // ===== 북마크 추가/삭제 =====

  /**
   * 게시글 북마크 추가
   *
   * @param userId 사용자 ID
   * @param postId 게시글 ID
   * @return 북마크 응답
   */
  @Transactional
  public BookmarkResponse bookmark(Long userId, Long postId) {
    log.info("북마크 추가 요청 - userId: {}, postId: {}", userId, postId);

    // 1. 게시글 존재 확인
    Post post = postRepository.findByIdAndIsDeletedFalse(postId)
        .orElseThrow(() -> new PostNotFoundException(postId));

    // 2. 중복 북마크 확인
    if (bookmarkRepository.existsByUserIdAndPostId(userId, postId)) {
      throw new DuplicateBookmarkException();
    }

    // 3. 북마크 생성
    User user = userRepository.getReferenceById(userId);
    Bookmark bookmark = Bookmark.create(user, post);
    bookmarkRepository.save(bookmark);

    log.info("북마크 추가 완료 - userId: {}, postId: {}", userId, postId);

    return BookmarkResponse.bookmarked(postId);
  }

  /**
   * 게시글 북마크 삭제
   *
   * @param userId 사용자 ID
   * @param postId 게시글 ID
   * @return 북마크 삭제 응답
   */
  @Transactional
  public BookmarkResponse unbookmark(Long userId, Long postId) {
    log.info("북마크 삭제 요청 - userId: {}, postId: {}", userId, postId);

    // 1. 북마크 존재 확인
    Bookmark bookmark = bookmarkRepository.findByUserIdAndPostId(userId, postId)
        .orElseThrow(() -> new BookmarkNotFoundException("북마크하지 않은 게시글입니다."));

    // 2. 북마크 삭제
    bookmarkRepository.delete(bookmark);

    log.info("북마크 삭제 완료 - userId: {}, postId: {}", userId, postId);

    return BookmarkResponse.unbookmarked(postId);
  }

  // ===== 북마크한 게시글 목록 조회 =====

  /**
   * 내가 북마크한 게시글 목록 조회
   *
   * @param userId 사용자 ID
   * @param pageable 페이지 정보
   * @return 북마크한 게시글 페이지
   */
  @Transactional(readOnly = true)
  public Page<BookmarkedPostResponse> getBookmarkedPosts(Long userId, Pageable pageable) {
    log.info("북마크 목록 조회 - userId: {}, page: {}", userId, pageable.getPageNumber());

    Page<Bookmark> bookmarks = bookmarkRepository.findByUserIdWithPost(userId, pageable);

    return bookmarks.map(BookmarkedPostResponse::from);
  }

  // ===== 북마크 여부 확인 =====

  /**
   * 게시글 북마크 여부 확인
   *
   * @param userId 사용자 ID
   * @param postId 게시글 ID
   * @return 북마크 여부
   */
  @Transactional(readOnly = true)
  public boolean isBookmarked(Long userId, Long postId) {
    return bookmarkRepository.existsByUserIdAndPostId(userId, postId);
  }

  // ===== 북마크 카운트 =====

  /**
   * 사용자의 북마크 수 조회
   *
   * @param userId 사용자 ID
   * @return 북마크 수
   */
  @Transactional(readOnly = true)
  public long getBookmarkCount(Long userId) {
    return bookmarkRepository.countByUserId(userId);
  }
}
