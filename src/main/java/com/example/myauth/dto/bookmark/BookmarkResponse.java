package com.example.myauth.dto.bookmark;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 북마크 응답 DTO
 * 북마크 추가/삭제 후 반환
 *
 * 【응답 예시】
 * {
 *   "postId": 1,
 *   "bookmarked": true
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkResponse {

  /**
   * 게시글 ID
   */
  private Long postId;

  /**
   * 북마크 상태
   * - true: 북마크됨
   * - false: 북마크 해제됨
   */
  private Boolean bookmarked;

  /**
   * 북마크 추가 응답 생성
   */
  public static BookmarkResponse bookmarked(Long postId) {
    return BookmarkResponse.builder()
        .postId(postId)
        .bookmarked(true)
        .build();
  }

  /**
   * 북마크 삭제 응답 생성
   */
  public static BookmarkResponse unbookmarked(Long postId) {
    return BookmarkResponse.builder()
        .postId(postId)
        .bookmarked(false)
        .build();
  }
}
