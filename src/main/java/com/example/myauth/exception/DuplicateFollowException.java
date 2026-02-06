package com.example.myauth.exception;

/**
 * 중복 팔로우 시도 시 발생하는 예외
 * - 이미 팔로우한 사용자를 다시 팔로우하려는 경우
 */
public class DuplicateFollowException extends RuntimeException {

  public DuplicateFollowException(String message) {
    super(message);
  }

  public DuplicateFollowException() {
    super("이미 팔로우한 사용자입니다.");
  }
}
