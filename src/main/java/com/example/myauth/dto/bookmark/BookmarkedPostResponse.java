package com.example.myauth.dto.bookmark;

import com.example.myauth.dto.post.PostAuthorResponse;
import com.example.myauth.entity.Bookmark;
import com.example.myauth.entity.Post;
import com.example.myauth.entity.Visibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 북마크한 게시글 응답 DTO
 * 북마크 목록 조회 시 반환
 *
 * 【응답 예시】
 * {
 *   "bookmarkId": 1,
 *   "bookmarkedAt": "2025-01-24T10:30:00",
 *   "post": {
 *     "id": 1,
 *     "content": "게시글 내용...",
 *     "thumbnailUrl": "http://...",
 *     "likeCount": 42,
 *     "commentCount": 5,
 *     "author": {
 *       "id": 1,
 *       "name": "홍길동",
 *       "profileImage": "http://..."
 *     },
 *     "createdAt": "2025-01-24T09:00:00"
 *   }
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkedPostResponse {

  /**
   * 북마크 ID
   */
  private Long bookmarkId;

  /**
   * 북마크한 일시
   */
  private LocalDateTime bookmarkedAt;

  /**
   * 게시글 정보
   */
  private PostSummary post;

  /**
   * 게시글 요약 정보 (내부 클래스)
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PostSummary {
    private Long id;
    private String content;
    private Visibility visibility;
    private String thumbnailUrl;
    private Integer imageCount;
    private Integer likeCount;
    private Integer commentCount;
    private PostAuthorResponse author;
    private LocalDateTime createdAt;
  }

  /**
   * Bookmark 엔티티 → DTO 변환
   */
  public static BookmarkedPostResponse from(Bookmark bookmark) {
    Post post = bookmark.getPost();

    // 본문 미리보기 (최대 100자)
    String contentPreview = post.getContent();
    if (contentPreview != null && contentPreview.length() > 100) {
      contentPreview = contentPreview.substring(0, 100) + "...";
    }

    // 첫 번째 이미지의 썸네일 URL
    String thumbnailUrl = null;
    if (!post.getImages().isEmpty()) {
      thumbnailUrl = post.getImages().get(0).getThumbnailUrl();
      if (thumbnailUrl == null) {
        thumbnailUrl = post.getImages().get(0).getImageUrl();
      }
    }

    PostSummary postSummary = PostSummary.builder()
        .id(post.getId())
        .content(contentPreview)
        .visibility(post.getVisibility())
        .thumbnailUrl(thumbnailUrl)
        .imageCount(post.getImages().size())
        .likeCount(post.getLikeCount())
        .commentCount(post.getCommentCount())
        .author(PostAuthorResponse.from(post.getUser()))
        .createdAt(post.getCreatedAt())
        .build();

    return BookmarkedPostResponse.builder()
        .bookmarkId(bookmark.getId())
        .bookmarkedAt(bookmark.getCreatedAt())
        .post(postSummary)
        .build();
  }
}
