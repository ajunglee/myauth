package com.example.myauth.service;

import com.example.myauth.dto.ImageUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * AWS S3에 이미지를 저장하는 서비스 (예시용)
 * 실제 프로덕션에서는 AWS SDK를 사용하여 구현
 */
@Service  // ⚠️ 이렇게 하면 LocalImageStorageService와 충돌 발생!
@Slf4j
public class S3ImageStorageService implements ImageStorageService {

  @Override
  public ImageUploadResponse store(MultipartFile file) {
    log.info("🚀 S3에 이미지 업로드: {}", file.getOriginalFilename());

    // 실제로는 AWS S3 SDK를 사용하여 업로드
    // 여기서는 시뮬레이션만
    String s3Url = "https://my-bucket.s3.amazonaws.com/" + file.getOriginalFilename();

    return ImageUploadResponse.builder()
        .imageUrl(s3Url)
        .fileName(file.getOriginalFilename())
        .originalFileName(file.getOriginalFilename())
        .fileSize(file.getSize())
        .contentType(file.getContentType())
        .build();
  }

  @Override
  public void delete(String fileName) {
    log.info("🗑️ S3에서 이미지 삭제: {}", fileName);
    // AWS S3에서 삭제 로직
  }
}
