package com.example.myauth.exception;

/**
 * 파일 유효성 검사 실패 예외
 * - 파일이 비어있음
 * - 파일 크기 초과
 * - 지원하지 않는 파일 형식
 * - 잘못된 파일명
 */
public class InvalidFileException extends RuntimeException {

  /** 에러 코드 (클라이언트에서 에러 종류 구분용) */
  private final ErrorCode errorCode;

  /**
   * 파일 유효성 에러 코드
   */
  public enum ErrorCode {
    /** 파일이 비어있거나 null */
    EMPTY_FILE,
    /** 파일 크기 초과 */
    FILE_TOO_LARGE,
    /** 지원하지 않는 파일 형식 */
    UNSUPPORTED_TYPE,
    /** 잘못된 파일명 (경로 조작 시도 등) */
    INVALID_FILENAME,
    /** 잘못된 파일 경로 */
    INVALID_PATH
  }

  public InvalidFileException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }
}