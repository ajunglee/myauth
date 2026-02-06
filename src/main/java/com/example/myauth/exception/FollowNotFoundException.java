package com.example.myauth.exception;

/**
 * 팔로우 관계를 찾을 수 없을 때 발생하는 예외
 * - 팔로우하지 않은 사용자를 언팔로우하려는 경우
 */
public class FollowNotFoundException extends RuntimeException {

  public FollowNotFoundException(String message) {
    super(message);
  }

  public FollowNotFoundException() {
    super("팔로우 관계를 찾을 수 없습니다.");
  }
}
