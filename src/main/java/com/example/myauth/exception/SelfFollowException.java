package com.example.myauth.exception;

/**
 * 자기 자신을 팔로우하려 할 때 발생하는 예외
 */
public class SelfFollowException extends RuntimeException {

  public SelfFollowException(String message) {
    super(message);
  }

  public SelfFollowException() {
    super("자기 자신을 팔로우할 수 없습니다.");
  }
}
