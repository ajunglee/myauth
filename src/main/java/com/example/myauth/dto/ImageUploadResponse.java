package com.example.myauth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 이미지 업로드 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadResponse {

  /**
   * 업로드된 이미지의 URL
   * 클라이언트에서 이 URL로 이미지에 접근할 수 있음
   */
  private String imageUrl;

  /**
   * 저장된 파일명 (UUID 기반)
   */
  private String fileName;

  /**
   * 원본 파일명
   */
  private String originalFileName;

  /**
   * 파일 크기 (bytes)
   */
  private Long fileSize;

  /**
   * 파일 타입 (image/jpeg, image/png 등)
   */
  private String contentType;
}
