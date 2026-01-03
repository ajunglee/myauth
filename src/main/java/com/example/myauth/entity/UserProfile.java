package com.example.myauth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자 프로필 정보 엔티티
 * users 테이블과 1:1 관계
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "user_profile")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 사용자 ID (Foreign Key) */
    @Column(nullable = false, unique = true)
    private Long user;

    /** 성 */
    @Column(name = "last_name", length = 100)
    private String lastName;

    /** 이름 */
    @Column(name = "first_name", length = 100)
    private String firstName;

    /** 전화번호 */
    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    /** 국가 코드 (기본값: 1) */
    @Column(nullable = false)
    @Builder.Default
    private Long country = 1L;

    /** 주소 1 */
    @Column(length = 100)
    private String address1;

    /** 주소 2 */
    @Column(length = 100)
    private String address2;

    /** 생년월일 */
    private LocalDateTime birth;

    /** 배경 이미지 URL */
    @Column(name = "bg_image", length = 500)
    private String bgImage;

    /** 생성 시간 */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 수정 시간 */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
