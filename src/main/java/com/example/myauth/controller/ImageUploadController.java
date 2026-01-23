package com.example.myauth.controller;

import com.example.myauth.dto.ApiResponse;
import com.example.myauth.dto.ImageUploadResponse;
import com.example.myauth.entity.User;
import com.example.myauth.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 업로드 관련 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/upload")  // API 일관성을 위해 /api 접두사 추가
@RequiredArgsConstructor
public class ImageUploadController {

  private final ImageStorageService imageStorageService;

  /**
   * ═══════════════════════════════════════════════════════════════════════════
   * 이미지 업로드 API 엔드포인트
   * ═══════════════════════════════════════════════════════════════════════════
   *
   * 【용도】
   * - 프로필 이미지 업로드
   * - 배경 이미지 업로드
   * - 기타 모든 이미지 파일 업로드
   *
   * 【HTTP 요청 형식】
   * - URL: POST /api/upload/image
   * - Content-Type: multipart/form-data
   * - Authorization: Bearer {JWT토큰} (필수)
   *
   * 【요청 예시 (cURL)】
   * curl -X POST http://localhost:9080/api/upload/image \
   *   -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..." \
   *   -F "file=@/path/to/image.jpg"
   *
   * 【응답 예시】
   * {
   *   "success": true,
   *   "message": "이미지가 성공적으로 업로드되었습니다.",
   *   "data": {
   *     "imageUrl": "http://localhost:9080/uploads/abc-123-def.jpg",
   *     "fileName": "abc-123-def.jpg",
   *     "originalFileName": "my-photo.jpg",
   *     "fileSize": 245678,
   *     "contentType": "image/jpeg"
   *   }
   * }
   *
   * @param user 현재 로그인한 사용자 (JWT 토큰에서 자동 추출됨)
   * @param file 업로드할 이미지 파일 (multipart/form-data의 "file" 파라미터)
   * @return 업로드된 이미지 정보를 담은 ApiResponse (이미지 URL 포함)
   */
  /*
   * ┌─────────────────────────────────────────────────────────────────────────┐
   * │ @PostMapping 어노테이션 설명                                              │
   * ├─────────────────────────────────────────────────────────────────────────┤
   * │ value = "/image"                                                        │
   * │   → 이 메서드가 처리할 URL 경로                                           │
   * │   → 클래스의 @RequestMapping("/api/upload")과 결합되어                    │
   * │   → 최종 URL: POST /api/upload/image                                    │
   * │                                                                         │
   * │ consumes = MediaType.MULTIPART_FORM_DATA_VALUE                          │
   * │   → 이 API가 받아들이는 Content-Type 지정                                 │
   * │   → "multipart/form-data" 형식만 허용                                   │
   * │   → 파일 업로드 시 반드시 이 Content-Type 사용해야 함                      │
   * │   → 일반 JSON(application/json)으로 요청하면 415 에러 발생                │
   * └─────────────────────────────────────────────────────────────────────────┘
   */
  @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  /*
   * ┌─────────────────────────────────────────────────────────────────────────┐
   * │ 반환 타입 설명: ResponseEntity<ApiResponse<ImageUploadResponse>>         │
   * ├─────────────────────────────────────────────────────────────────────────┤
   * │ ResponseEntity<T>                                                       │
   * │   → HTTP 응답 전체를 표현하는 Spring 클래스                               │
   * │   → HTTP 상태 코드, 헤더, 바디를 모두 제어 가능                           │
   * │   → ResponseEntity.ok()는 200 OK 상태 코드로 응답                        │
   * │                                                                         │
   * │ ApiResponse<T>                                                          │
   * │   → 우리가 정의한 공통 응답 DTO                                           │
   * │   → { success: boolean, message: string, data: T } 형태                 │
   * │   → 모든 API가 일관된 응답 형식 유지                                      │
   * │                                                                         │
   * │ ImageUploadResponse                                                     │
   * │   → 이미지 업로드 결과를 담는 DTO                                         │
   * │   → imageUrl, fileName, originalFileName, fileSize, contentType 포함   │
   * └─────────────────────────────────────────────────────────────────────────┘
   */
  public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImage(
      /*
       * ┌───────────────────────────────────────────────────────────────────────┐
       * │ @AuthenticationPrincipal User user                                    │
       * ├───────────────────────────────────────────────────────────────────────┤
       * │ @AuthenticationPrincipal                                              │
       * │   → Spring Security가 제공하는 어노테이션                               │
       * │   → SecurityContext에서 현재 인증된 사용자 정보를 자동 주입             │
       * │   → JWT 토큰에서 추출한 사용자 정보가 자동으로 이 파라미터에 바인딩됨     │
       * │                                                                       │
       * │ 동작 원리:                                                             │
       * │   1. 클라이언트가 Authorization 헤더에 JWT 토큰 전송                   │
       * │   2. JwtAuthenticationFilter가 토큰 검증                              │
       * │   3. 토큰에서 사용자 ID 추출 후 DB에서 User 조회                       │
       * │   4. SecurityContextHolder에 인증 정보 저장                           │
       * │   5. @AuthenticationPrincipal이 SecurityContext에서 User 객체 추출    │
       * │                                                                       │
       * │ User user                                                             │
       * │   → 현재 로그인한 사용자의 엔티티 객체                                  │
       * │   → user.getId(), user.getEmail() 등으로 사용자 정보 접근 가능         │
       * │   → 인증되지 않은 요청은 이 메서드에 도달하기 전에 401 에러 반환        │
       * └───────────────────────────────────────────────────────────────────────┘
       */
      @AuthenticationPrincipal User user,
      /*
       * ┌───────────────────────────────────────────────────────────────────────┐
       * │ @RequestParam("file") MultipartFile file                              │
       * ├───────────────────────────────────────────────────────────────────────┤
       * │ @RequestParam("file")                                                 │
       * │   → HTTP 요청의 파라미터를 메서드 파라미터에 바인딩                     │
       * │   → "file"은 클라이언트가 전송하는 폼 필드 이름                        │
       * │   → 예: <input type="file" name="file" /> 또는                        │
       * │   → FormData.append('file', fileObject)                              │
       * │                                                                       │
       * │ MultipartFile                                                         │
       * │   → Spring이 제공하는 업로드 파일 인터페이스                            │
       * │   → 업로드된 파일의 내용과 메타데이터에 접근 가능                       │
       * │                                                                       │
       * │ MultipartFile 주요 메서드:                                             │
       * │   → getOriginalFilename(): 원본 파일명 (예: "photo.jpg")              │
       * │   → getSize(): 파일 크기 (bytes)                                      │
       * │   → getContentType(): MIME 타입 (예: "image/jpeg")                    │
       * │   → getInputStream(): 파일 내용을 읽기 위한 스트림                     │
       * │   → isEmpty(): 파일이 비어있는지 확인                                  │
       * │   → getBytes(): 파일 내용을 byte 배열로 반환                           │
       * │   → transferTo(File dest): 파일을 지정된 경로에 저장                   │
       * └───────────────────────────────────────────────────────────────────────┘
       */
      @RequestParam("file") MultipartFile file
  ) {
    /*
     * ┌─────────────────────────────────────────────────────────────────────────┐
     * │ 로그 기록 - 요청 시작                                                    │
     * ├─────────────────────────────────────────────────────────────────────────┤
     * │ log.info()                                                              │
     * │   → Slf4j 로깅 (INFO 레벨)                                              │
     * │   → 운영 환경에서 요청 추적 및 디버깅에 활용                              │
     * │   → {}는 플레이스홀더로, 뒤의 인자들이 순서대로 치환됨                    │
     * │                                                                         │
     * │ 기록 정보:                                                               │
     * │   → user.getId(): 요청한 사용자 ID (누가 업로드했는지 추적)              │
     * │   → file.getOriginalFilename(): 원본 파일명                             │
     * │   → file.getSize(): 파일 크기 (bytes 단위)                              │
     * └─────────────────────────────────────────────────────────────────────────┘
     */
    log.info("이미지 업로드 요청 - userId: {}, 파일명: {}, 크기: {} bytes",
        user.getId(), file.getOriginalFilename(), file.getSize());

    /*
     * ┌─────────────────────────────────────────────────────────────────────────┐
     * │ 이미지 저장 처리                                                         │
     * ├─────────────────────────────────────────────────────────────────────────┤
     * │ imageStorageService.store(file)                                         │
     * │   → ImageStorageService 인터페이스의 store 메서드 호출                   │
     * │   → 실제로는 LocalImageStorageService 구현체가 실행됨 (@Primary)        │
     * │                                                                         │
     * │ store() 메서드 내부 동작:                                                │
     * │   1. 파일 유효성 검사 (타입, 크기, 보안)                                 │
     * │   2. 업로드 디렉토리 확인/생성                                           │
     * │   3. UUID 기반 고유 파일명 생성                                          │
     * │   4. 파일 시스템에 저장                                                  │
     * │   5. 접근 가능한 URL 생성                                                │
     * │                                                                         │
     * │ 예외 처리:                                                               │
     * │   → 파일이 유효하지 않으면 RuntimeException 발생                         │
     * │   → GlobalExceptionHandler가 예외를 잡아서 적절한 HTTP 응답 반환         │
     * │   → try-catch가 없는 이유: 중앙 집중식 예외 처리 패턴 사용               │
     * └─────────────────────────────────────────────────────────────────────────┘
     */
    ImageUploadResponse response = imageStorageService.store(file);

    /*
     * ┌─────────────────────────────────────────────────────────────────────────┐
     * │ 로그 기록 - 요청 완료                                                    │
     * ├─────────────────────────────────────────────────────────────────────────┤
     * │ 업로드 성공 시 로그 기록                                                 │
     * │   → userId: 누가 업로드했는지                                            │
     * │   → imageUrl: 생성된 이미지 URL (디버깅 및 추적용)                       │
     * │                                                                         │
     * │ 이 로그는 store()가 성공적으로 완료된 후에만 실행됨                       │
     * │ (예외 발생 시 이 라인에 도달하지 않음)                                    │
     * └─────────────────────────────────────────────────────────────────────────┘
     */
    log.info("이미지 업로드 성공 - userId: {}, imageUrl: {}", user.getId(), response.getImageUrl());

    /*
     * ┌─────────────────────────────────────────────────────────────────────────┐
     * │ HTTP 응답 반환                                                           │
     * ├─────────────────────────────────────────────────────────────────────────┤
     * │ ResponseEntity.ok(...)                                                  │
     * │   → HTTP 200 OK 상태 코드로 응답 생성                                    │
     * │   → 괄호 안의 객체가 응답 바디(body)가 됨                                │
     * │                                                                         │
     * │ ApiResponse.success(message, data)                                      │
     * │   → 성공 응답 DTO 생성 팩토리 메서드                                     │
     * │   → success: true                                                       │
     * │   → message: "이미지가 성공적으로 업로드되었습니다."                      │
     * │   → data: ImageUploadResponse 객체                                      │
     * │                                                                         │
     * │ 최종 응답 JSON:                                                          │
     * │ {                                                                        │
     * │   "success": true,                                                       │
     * │   "message": "이미지가 성공적으로 업로드되었습니다.",                     │
     * │   "data": {                                                              │
     * │     "imageUrl": "http://서버/uploads/uuid.jpg",                          │
     * │     "fileName": "uuid.jpg",                                              │
     * │     "originalFileName": "원본파일명.jpg",                                │
     * │     "fileSize": 12345,                                                   │
     * │     "contentType": "image/jpeg"                                          │
     * │   }                                                                      │
     * │ }                                                                        │
     * └─────────────────────────────────────────────────────────────────────────┘
     */
    return ResponseEntity.ok(ApiResponse.success("이미지가 성공적으로 업로드되었습니다.", response));
  }

  /**
   * 이미지 삭제 엔드포인트 (선택적)
   * 필요시 활성화
   *
   * @param user 현재 로그인한 사용자
   * @param fileName 삭제할 파일명
   * @return 삭제 결과
   */
  @DeleteMapping("/image/{fileName}")
  public ResponseEntity<ApiResponse<Void>> deleteImage(
      @AuthenticationPrincipal User user,
      @PathVariable String fileName
  ) {
    log.info("이미지 삭제 요청 - userId: {}, fileName: {}", user.getId(), fileName);

    // 이미지 삭제 (실패 시 예외 던짐 - GlobalExceptionHandler에서 처리)
    imageStorageService.delete(fileName);

    log.info("이미지 삭제 성공 - userId: {}, fileName: {}", user.getId(), fileName);

    return ResponseEntity.ok(ApiResponse.success("이미지가 성공적으로 삭제되었습니다.", null));
  }
}
