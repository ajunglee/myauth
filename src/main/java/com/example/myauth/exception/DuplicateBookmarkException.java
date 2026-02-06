package com.example.myauth.exception;

/**
 * 중복 북마크 시도 시 발생하는 예외
 * - 이미 북마크한 게시글을 다시 북마크하려는 경우
 */
public class DuplicateBookmarkException extends RuntimeException {

  public DuplicateBookmarkException(String message) {
    super(message);
  }

  public DuplicateBookmarkException() {
    super("이미 북마크한 게시글입니다.");
  }
}
