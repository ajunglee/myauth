package com.example.myauth.exception;

/**
 * 사용자를 찾을 수 없을 때 발생하는 예외
 * - 존재하지 않는 사용자 ID로 조회/팔로우 시도
 */
public class UserNotFoundException extends RuntimeException {

  public UserNotFoundException(String message) {
    super(message);
  }

  public UserNotFoundException(Long userId) {
    super("사용자를 찾을 수 없습니다. (ID: " + userId + ")");
  }
}
