package com.example.myauth.exception;

/**
 * 파일 저장/삭제 관련 예외
 * - 파일 저장 실패
 * - 파일 삭제 실패
 * - 디렉토리 생성 실패
 */
public class FileStorageException extends RuntimeException {

  public FileStorageException(String message) {
    super(message);
  }

  public FileStorageException(String message, Throwable cause) {
    super(message, cause);
  }
}