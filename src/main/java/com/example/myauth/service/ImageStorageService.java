package com.example.myauth.service;

import com.example.myauth.dto.ImageUploadResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 저장 서비스 인터페이스
 * 나중에 AWS S3 등 다른 스토리지로 교체 가능하도록 인터페이스로 분리
 */
public interface ImageStorageService {

  /**
   * 이미지 파일을 저장하고 접근 가능한 URL을 반환
   *
   * @param file 업로드된 이미지 파일
   * @return 저장된 이미지 정보 (URL 포함)
   * @throws RuntimeException 파일 저장 실패 시
   */
  ImageUploadResponse store(MultipartFile file);

  /**
   * 저장된 이미지 파일을 삭제
   *
   * @param fileName 삭제할 파일명
   * @throws RuntimeException 파일 삭제 실패 시
   */
  void delete(String fileName);
}
