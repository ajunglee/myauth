package com.example.myauth.controller;

import com.example.myauth.dto.ApiResponse;
import com.example.myauth.dto.bookmark.BookmarkResponse;
import com.example.myauth.dto.bookmark.BookmarkedPostResponse;
import com.example.myauth.entity.User;
import com.example.myauth.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 북마크 컨트롤러
 * 게시글 북마크(저장) 관련 API 엔드포인트 제공
 *
 * 【API 목록】
 * - POST   /api/posts/{postId}/bookmark    : 북마크 추가
 * - DELETE /api/posts/{postId}/bookmark    : 북마크 삭제
 * - GET    /api/me/bookmarks               : 내 북마크 목록
 * - GET    /api/posts/{postId}/bookmark/check : 북마크 여부 확인
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class BookmarkController {

  private final BookmarkService bookmarkService;

  // ===== 북마크 추가/삭제 =====

  /**
   * 게시글 북마크 추가
   *
   * POST /api/posts/{postId}/bookmark
   *
   * 【응답 예시】
   * {
   *   "success": true,
   *   "message": "북마크 완료",
   *   "data": {
   *     "postId": 1,
   *     "bookmarked": true
   *   }
   * }
   */
  @PostMapping("/api/posts/{postId}/bookmark")
  public ResponseEntity<ApiResponse<BookmarkResponse>> bookmark(
      @AuthenticationPrincipal User user,
      @PathVariable Long postId
  ) {
    log.info("북마크 추가 요청 - userId: {}, postId: {}", user.getId(), postId);

    BookmarkResponse response = bookmarkService.bookmark(user.getId(), postId);

    return ResponseEntity.ok(ApiResponse.success("북마크 완료", response));
  }

  /**
   * 게시글 북마크 삭제
   *
   * DELETE /api/posts/{postId}/bookmark
   */
  @DeleteMapping("/api/posts/{postId}/bookmark")
  public ResponseEntity<ApiResponse<BookmarkResponse>> unbookmark(
      @AuthenticationPrincipal User user,
      @PathVariable Long postId
  ) {
    log.info("북마크 삭제 요청 - userId: {}, postId: {}", user.getId(), postId);

    BookmarkResponse response = bookmarkService.unbookmark(user.getId(), postId);

    return ResponseEntity.ok(ApiResponse.success("북마크 해제 완료", response));
  }

  // ===== 북마크 목록 조회 =====

  /**
   * 내 북마크 목록 조회
   *
   * GET /api/me/bookmarks?page=0&size=10
   *
   * 【쿼리 파라미터】
   * - page: 페이지 번호 (0부터 시작, 기본값 0)
   * - size: 페이지 크기 (기본값 10, 최대 50)
   */
  @GetMapping("/api/me/bookmarks")
  public ResponseEntity<ApiResponse<Page<BookmarkedPostResponse>>> getMyBookmarks(
      @AuthenticationPrincipal User user,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size
  ) {
    log.info("내 북마크 목록 조회 - userId: {}", user.getId());

    // 페이지 크기 제한
    if (size > 50) size = 50;

    Pageable pageable = PageRequest.of(page, size);
    Page<BookmarkedPostResponse> bookmarks = bookmarkService.getBookmarkedPosts(user.getId(), pageable);

    return ResponseEntity.ok(ApiResponse.success("북마크 목록 조회 성공", bookmarks));
  }

  // ===== 북마크 여부 확인 =====

  /**
   * 게시글 북마크 여부 확인
   *
   * GET /api/posts/{postId}/bookmark/check
   *
   * 【응답 예시】
   * {
   *   "bookmarked": true
   * }
   */
  @GetMapping("/api/posts/{postId}/bookmark/check")
  public ResponseEntity<ApiResponse<Boolean>> checkBookmark(
      @AuthenticationPrincipal User user,
      @PathVariable Long postId
  ) {
    log.info("북마크 여부 확인 - userId: {}, postId: {}", user.getId(), postId);

    boolean isBookmarked = bookmarkService.isBookmarked(user.getId(), postId);

    return ResponseEntity.ok(ApiResponse.success("북마크 여부 확인 성공", isBookmarked));
  }
}
