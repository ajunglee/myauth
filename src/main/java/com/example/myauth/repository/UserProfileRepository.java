package com.example.myauth.repository;

import com.example.myauth.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 프로필 정보를 관리하는 Repository
 * Spring Data JPA가 자동으로 구현을 생성한다
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
  /**
   * 사용자 ID로 프로필을 조회한다
   * @param userId 사용자 ID (users 테이블의 id)
   * @return 사용자 프로필 정보 (Optional)
   */
  Optional<UserProfile> findByUser(Long userId);

  /**
   * 사용자 ID로 프로필이 존재하는지 확인한다
   * @param userId 사용자 ID
   * @return 존재하면 true, 아니면 false
   */
  boolean existsByUser(Long userId);
}
