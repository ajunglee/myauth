package com.example.myauth.exception;

/**
 * 북마크 기록을 찾을 수 없을 때 발생하는 예외
 * - 북마크하지 않은 게시글을 북마크 취소하려는 경우
 */
public class BookmarkNotFoundException extends RuntimeException {

  public BookmarkNotFoundException(String message) {
    super(message);
  }

  public BookmarkNotFoundException() {
    super("북마크 기록을 찾을 수 없습니다.");
  }
}
