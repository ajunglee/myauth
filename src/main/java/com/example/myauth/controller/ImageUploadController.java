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
   * 이미지 업로드 엔드포인트
   * 프로필 이미지, 배경 이미지 등 모든 이미지 업로드에 사용
   *
   * @param user 현재 로그인한 사용자 (JWT 토큰에서 추출됨)
   * @param file 업로드할 이미지 파일
   * @return 업로드된 이미지 정보 (URL 포함)
   */
  @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImage(
      @AuthenticationPrincipal User user,
      @RequestParam("file") MultipartFile file
  ) {
    log.info("이미지 업로드 요청 - userId: {}, 파일명: {}, 크기: {} bytes",
        user.getId(), file.getOriginalFilename(), file.getSize());

    // 이미지 저장 및 URL 생성 (실패 시 예외 던짐 - GlobalExceptionHandler에서 처리)
    ImageUploadResponse response = imageStorageService.store(file);

    log.info("이미지 업로드 성공 - userId: {}, imageUrl: {}", user.getId(), response.getImageUrl());

    // 성공 응답 반환
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
