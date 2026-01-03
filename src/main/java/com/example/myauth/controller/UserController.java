package com.example.myauth.controller;

import com.example.myauth.dto.ApiResponse;
import com.example.myauth.dto.UserProfileUpdateRequest;
import com.example.myauth.dto.UserProfileUpdateResponse;
import com.example.myauth.entity.User;
import com.example.myauth.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 정보 관련 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/user")  // API 일관성을 위해 /api 접두사 추가
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  /**
   * 현재 로그인한 사용자 정보 조회
   * JWT Access Token을 통해 인증된 사용자의 전체 정보를 반환
   *
   * @param user SecurityContext에서 자동으로 주입되는 현재 로그인한 사용자 (JWT 토큰에서 추출됨)
   * @return 사용자 정보를 포함한 ApiResponse
   */
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<Map<String, Object>>> me(
      @AuthenticationPrincipal User user
  ) {
    log.info("현재 사용자 정보 조회 요청: {}", user.getEmail());

    // 사용자 정보를 Map으로 구성 (카카오 OAuth 정보 포함)
    Map<String, Object> userInfo = new HashMap<>();
    userInfo.put("id", user.getId());
    userInfo.put("email", user.getEmail());
    userInfo.put("name", user.getName());
    userInfo.put("profileImage", user.getProfileImage());  // 프로필 이미지 추가
    userInfo.put("provider", user.getProvider());  // OAuth 제공자 (KAKAO 등) 추가
    userInfo.put("role", user.getRole().name());
    userInfo.put("status", user.getStatus().name());  // 계정 상태 추가
    userInfo.put("isActive", user.getIsActive());
    userInfo.put("createdAt", user.getCreatedAt());  // 계정 생성일 추가

    // 응답 생성
    ApiResponse<Map<String, Object>> response =
        ApiResponse.success("사용자 정보 조회 성공", userInfo);

    return ResponseEntity.ok(response);
  }

  /**
   * 사용자 프로필 정보 수정
   * User 테이블과 UserProfile 테이블의 정보를 동시에 수정
   *
   * @param user 현재 로그인한 사용자 (JWT 토큰에서 추출됨)
   * @param request 프로필 수정 요청 DTO
   * @return 수정된 프로필 정보를 포함한 ApiResponse
   */
  @PutMapping("/profile")
  public ResponseEntity<ApiResponse<UserProfileUpdateResponse>> updateProfile(
      @AuthenticationPrincipal User user,
      @RequestBody UserProfileUpdateRequest request
  ) {
    log.info("사용자 프로필 수정 요청: userId={}, email={}", user.getId(), user.getEmail());

    // UserService를 통해 프로필 수정
    UserProfileUpdateResponse response = userService.updateUserProfile(user.getId(), request);

    log.info("사용자 프로필 수정 완료: userId={}", user.getId());

    // 응답 생성
    ApiResponse<UserProfileUpdateResponse> apiResponse =
        ApiResponse.success("프로필이 성공적으로 수정되었습니다.", response);

    return ResponseEntity.ok(apiResponse);
  }
}
