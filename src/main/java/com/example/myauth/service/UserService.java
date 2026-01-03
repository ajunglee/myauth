package com.example.myauth.service;

import com.example.myauth.dto.UserProfileUpdateRequest;
import com.example.myauth.dto.UserProfileUpdateResponse;
import com.example.myauth.entity.User;
import com.example.myauth.entity.UserProfile;
import com.example.myauth.repository.UserProfileRepository;
import com.example.myauth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 관련 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;
  private final UserProfileRepository userProfileRepository;

  /**
   * 사용자 프로필 정보 수정
   * User 테이블과 UserProfile 테이블의 정보를 동시에 수정
   *
   * @param userId 사용자 ID
   * @param request 프로필 수정 요청 DTO
   * @return 수정된 프로필 정보
   * @throws RuntimeException 사용자를 찾을 수 없는 경우
   */
  @Transactional
  public UserProfileUpdateResponse updateUserProfile(Long userId, UserProfileUpdateRequest request) {
    log.info("사용자 프로필 수정 요청 - userId: {}", userId);

    // 1️⃣ User 조회
    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.warn("존재하지 않는 사용자 ID로 프로필 수정 시도: {}", userId);
          return new RuntimeException("사용자를 찾을 수 없습니다.");
        });

    // 2️⃣ User 정보 업데이트 (name, profileImage)
    if (request.getName() != null) {
      user.setName(request.getName());
      log.debug("사용자 이름(닉네임) 수정: {}", request.getName());
    }

    if (request.getProfileImage() != null) {
      user.setProfileImage(request.getProfileImage());
      log.debug("프로필 이미지 수정: {}", request.getProfileImage());
    }

    // User 저장 (변경 감지에 의해 자동으로 UPDATE 쿼리 실행)
    userRepository.save(user);

    // 3️⃣ UserProfile 조회 또는 생성
    UserProfile userProfile = userProfileRepository.findByUser(userId)
        .orElseGet(() -> {
          log.info("UserProfile이 존재하지 않아 새로 생성: userId={}", userId);
          return UserProfile.builder()
              .user(userId)
              .country(1L)  // 기본값: 1
              .build();
        });

    // 4️⃣ UserProfile 정보 업데이트
    if (request.getLastName() != null) {
      userProfile.setLastName(request.getLastName());
    }

    if (request.getFirstName() != null) {
      userProfile.setFirstName(request.getFirstName());
    }

    if (request.getPhoneNumber() != null) {
      userProfile.setPhoneNumber(request.getPhoneNumber());
    }

    if (request.getCountry() != null) {
      userProfile.setCountry(request.getCountry());
    }

    if (request.getAddress1() != null) {
      userProfile.setAddress1(request.getAddress1());
    }

    if (request.getAddress2() != null) {
      userProfile.setAddress2(request.getAddress2());
    }

    if (request.getBirth() != null) {
      userProfile.setBirth(request.getBirth());
    }

    if (request.getBgImage() != null) {
      userProfile.setBgImage(request.getBgImage());
    }

    // UserProfile 저장 (변경 감지에 의해 자동으로 UPDATE 쿼리 실행)
    userProfile = userProfileRepository.save(userProfile);

    log.info("사용자 프로필 수정 완료 - userId: {}", userId);

    // 5️⃣ 응답 DTO 생성 및 반환
    return UserProfileUpdateResponse.builder()
        .userId(user.getId())
        .email(user.getEmail())
        .name(user.getName())
        .profileImage(user.getProfileImage())
        .provider(user.getProvider())
        .profileId(userProfile.getId())
        .lastName(userProfile.getLastName())
        .firstName(userProfile.getFirstName())
        .phoneNumber(userProfile.getPhoneNumber())
        .country(userProfile.getCountry())
        .address1(userProfile.getAddress1())
        .address2(userProfile.getAddress2())
        .birth(userProfile.getBirth())
        .bgImage(userProfile.getBgImage())
        .createdAt(userProfile.getCreatedAt())
        .updatedAt(userProfile.getUpdatedAt())
        .build();
  }
}
