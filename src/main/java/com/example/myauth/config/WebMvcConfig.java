package com.example.myauth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Spring MVC 설정
 * 정적 리소스 매핑 등을 설정
 */
@Configuration
@Slf4j
public class WebMvcConfig implements WebMvcConfigurer {

  @Value("${file.upload.dir:./uploads}")
  private String uploadDir;

  /**
   * 정적 리소스 핸들러 등록
   * 업로드된 파일을 HTTP로 접근할 수 있도록 매핑
   *
   * 예: http://localhost:9080/uploads/abc-123.jpg
   *     -> ./uploads/abc-123.jpg 파일을 반환
   */
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // 업로드 디렉토리의 절대 경로 생성
    String absoluteUploadPath = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();

    log.info("정적 리소스 매핑 설정 - /uploads/** -> {}", absoluteUploadPath);

    // /uploads/** 경로로 들어오는 요청을 업로드 디렉토리로 매핑
    registry
        .addResourceHandler("/uploads/**")
        .addResourceLocations(absoluteUploadPath);
  }
}
