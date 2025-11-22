package com.example.myauth.security;

import com.example.myauth.entity.User;
import com.example.myauth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security의 UserDetailsService 인터페이스 구현체
 * Spring Security가 사용자 인증 시 이 서비스를 통해 사용자 정보를 로드한다
 *
 * UserDetailsService는 Spring Security의 핵심 인터페이스로,
 * username(우리는 email)을 받아서 UserDetails 객체를 반환하는 역할을 한다
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  /**
   * Spring Security가 인증 시 자동으로 호출하는 메서드
   * username(email)으로 사용자를 조회하여 UserDetails 객체를 반환한다
   *
   * @param username 사용자의 고유 식별자 (우리 시스템에서는 email)
   * @return UserDetails 구현체 (CustomUserDetails)
   * @throws UsernameNotFoundException 사용자를 찾을 수 없는 경우
   */
  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    log.debug("사용자 로드 시도: {}", username);

    // 이메일로 사용자 조회
    User user = userRepository.findByEmail(username)
        .orElseThrow(() -> {
          log.warn("사용자를 찾을 수 없음: {}", username);
          return new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username);
        });

    log.debug("사용자 로드 성공: {}, 상태: {}, 활성: {}",
        user.getEmail(), user.getStatus(), user.getIsActive());

    // User 엔티티를 CustomUserDetails로 래핑하여 반환
    return new CustomUserDetails(user);
  }

  /**
   * ID로 사용자를 로드하는 추가 메서드
   * JWT 필터 등에서 userId로 사용자를 조회할 때 사용한다
   *
   * @param userId 사용자 ID
   * @return UserDetails 구현체 (CustomUserDetails)
   * @throws UsernameNotFoundException 사용자를 찾을 수 없는 경우
   */
  public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
    log.debug("사용자 ID로 로드 시도: {}", userId);

    // ID로 사용자 조회
    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.warn("사용자를 찾을 수 없음 (ID: {})", userId);
          return new UsernameNotFoundException("사용자를 찾을 수 없습니다 (ID: " + userId + ")");
        });

    log.debug("사용자 로드 성공 (ID: {}): {}", userId, user.getEmail());

    // User 엔티티를 CustomUserDetails로 래핑하여 반환
    return new CustomUserDetails(user);
  }

  /**
   * 사용자 존재 여부 확인 (추가 유틸리티 메서드)
   *
   * @param email 이메일
   * @return 사용자 존재 여부
   */
  public boolean existsByEmail(String email) {
    return userRepository.findByEmail(email).isPresent();
  }
}
