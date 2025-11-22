package com.example.myauth.exception;

import com.example.myauth.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리 핸들러
 * 모든 @RestController에서 발생하는 예외를 한 곳에서 처리한다
 */
@Slf4j
@RestControllerAdvice   // Spring 전역 예외처리기로 등록
public class GlobalExceptionHandler {
  /**
   * Bean Validation 검증 실패 시 처리
   * Controller에서 @Valid 어노테이션으로 검증 실패한 경우 발생하는 예외를 처리한다
   *
   * @param ex 검증 실패 예외 객체
   * @return 첫 번째 에러 메시지를 포함한 ApiResponse
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @SuppressWarnings("NullableProblems")  // ApiResponse는 항상 non-null을 반환하므로 경고 억제
  public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(
      MethodArgumentNotValidException ex) {

    // 첫 번째 에러 메시지만 반환
    String errorMessage = ex.getBindingResult()
        .getAllErrors()
        .stream()
        .findFirst()
        .map(ObjectError::getDefaultMessage)
        .orElse("입력값이 올바르지 않습니다.");

    log.warn("입력값 검증 실패: {}", errorMessage);

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(errorMessage));
  }

  /**
   * 트랜잭션 롤백 예외 처리
   * @Transactional 메서드에서 예외를 catch해서 처리했지만 트랜잭션이 rollback-only로 마킹된 경우 발생
   *
   * @param ex UnexpectedRollbackException
   * @return 적절한 에러 메시지
   */
  @ExceptionHandler(UnexpectedRollbackException.class)
  @SuppressWarnings("NullableProblems")
  public ResponseEntity<ApiResponse<Void>> handleUnexpectedRollbackException(
      UnexpectedRollbackException ex) {

    log.warn("트랜잭션 롤백 예외 발생: {}", ex.getMessage());

    // 원인 예외를 재귀적으로 탐색
    Throwable cause = ex;
    while (cause != null) {
      log.debug("예외 체인: {}", cause.getClass().getName());

      // DataIntegrityViolationException 발견
      if (cause instanceof DataIntegrityViolationException) {
        log.warn("데이터 무결성 제약 위반 발견: {}", cause.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("이미 가입된 이메일입니다."));
      }

      cause = cause.getCause();
    }

    // DataIntegrityViolationException을 찾지 못한 경우
    // (회원가입 시 중복 이메일 에러가 대부분이므로 기본 메시지 제공)
    log.warn("원인 예외를 특정할 수 없지만, 중복 이메일로 추정됨");
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error("이미 가입된 이메일입니다."));
  }

  /**
   * 모든 예외를 처리하는 최후의 방어선
   * 다른 ExceptionHandler에서 처리되지 않은 모든 예외를 여기서 처리한다
   *
   * @param ex 발생한 예외 객체
   * @return 서버 오류 메시지를 포함한 ApiResponse
   */
  @ExceptionHandler(Exception.class)
  @SuppressWarnings("NullableProblems")
  public ResponseEntity<ApiResponse<Void>> handleAllExceptions(Exception ex) {
    // 스택 트레이스에서 예외 발생 위치 추출
    StackTraceElement[] stackTrace = ex.getStackTrace();
    String errorLocation = "알 수 없음";

    if (stackTrace != null && stackTrace.length > 0) {
      StackTraceElement firstElement = stackTrace[0];
      errorLocation = String.format("%s.%s (line: %d)",
          firstElement.getClassName(),
          firstElement.getMethodName(),
          firstElement.getLineNumber());
    }

    // 상세한 로그 기록 (개발자용)
    log.error("=== 예상치 못한 오류 발생 ===");
    log.error("예외 타입: {}", ex.getClass().getName());
    log.error("예외 메시지: {}", ex.getMessage());
    log.error("발생 위치: {}", errorLocation);
    log.error("전체 스택 트레이스:", ex);
    log.error("===========================");

    // 클라이언트에게는 간단한 메시지만 반환 (보안상 상세 정보는 숨김)
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
  }
}



