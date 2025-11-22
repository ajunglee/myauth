package com.example.myauth.security;

import com.example.myauth.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security의 UserDetails 인터페이스 구현체
 * 우리의 User 엔티티를 Spring Security가 이해할 수 있는 형식으로 변환한다
 *
 * UserDetails는 Spring Security가 인증/인가 시 사용하는 표준 인터페이스이다
 * 이 클래스를 통해 User 엔티티의 정보를 Spring Security에 제공한다
 */
@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

  /**
   * 우리의 User 엔티티
   * final로 선언하여 불변성 보장
   * -- GETTER --
   *  원본 User 엔티티를 반환하는 헬퍼 메서드
   *
   * @return User 엔티티

   */
  private final User user;

  /**
   * 사용자의 권한 목록을 반환
   * Spring Security가 인가(Authorization) 시 이 메서드를 호출한다
   *
   * @return 권한 목록 (예: ROLE_USER, ROLE_ADMIN)
   */
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    // User 엔티티의 role을 GrantedAuthority로 변환
    return List.of(new SimpleGrantedAuthority(user.getRole().name()));
  }

  /**
   * 사용자의 암호화된 비밀번호를 반환
   * Spring Security가 로그인 시 비밀번호 검증에 사용한다
   *
   * @return 암호화된 비밀번호
   */
  @Override
  public String getPassword() {
    return user.getPassword();
  }

  /**
   * 사용자의 고유 식별자를 반환 (일반적으로 username 또는 email)
   * Spring Security가 사용자를 식별하는 데 사용한다
   *
   * @return 이메일 (우리 시스템에서는 email을 username으로 사용)
   */
  @Override
  public String getUsername() {
    return user.getEmail();
  }

  /**
   * 계정이 만료되지 않았는지 확인
   * 우리 시스템에서는 계정 만료 기능이 없으므로 항상 true
   *
   * @return true면 계정이 유효함
   */
  @Override
  public boolean isAccountNonExpired() {
    return true;  // 계정 만료 기능 없음
  }

  /**
   * 계정이 잠기지 않았는지 확인
   * User 엔티티의 isActive 필드로 판단
   *
   * @return true면 계정이 잠기지 않음
   */
  @Override
  public boolean isAccountNonLocked() {
    // isActive가 false면 계정이 잠긴 것으로 간주
    return user.getIsActive();
  }

  /**
   * 자격 증명(비밀번호)이 만료되지 않았는지 확인
   * 우리 시스템에서는 비밀번호 만료 기능이 없으므로 항상 true
   *
   * @return true면 자격 증명이 유효함
   */
  @Override
  public boolean isCredentialsNonExpired() {
    return true;  // 비밀번호 만료 기능 없음
  }

  /**
   * 계정이 활성화되어 있는지 확인
   * User 엔티티의 status와 isActive로 판단
   *
   * @return true면 계정이 활성화됨
   */
  @Override
  public boolean isEnabled() {
    // ACTIVE 상태이고 isActive가 true일 때만 활성화
    return user.getStatus() == User.Status.ACTIVE && user.getIsActive();
  }
}
