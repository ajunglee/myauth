# 카카오 OAuth 로그인 구현 문서

> Spring Boot 4.0.0 + Spring Security 7.0.0 기반 카카오 소셜 로그인 구현

## 📋 목차

1. [개요](#개요)
2. [카카오 개발자 콘솔 설정](#🔧-카카오-개발자-콘솔-설정) ⚠️ **필수**
3. [데이터베이스 스키마 변경](#1️⃣-데이터베이스-스키마-변경)
4. [설정 파일 관리](#2️⃣-설정-파일-관리)
5. [DTO 구조 설계](#3️⃣-dto-구조-설계)
6. [카카오 OAuth 서비스 로직](#4️⃣-카카오-oauth-서비스-로직)
7. [컨트롤러 엔드포인트](#5️⃣-컨트롤러-엔드포인트)
8. [카카오 로그아웃](#6️⃣-카카오-로그아웃)
9. [Repository 메서드](#7️⃣-repository-메서드-추가)
10. [Spring Security 설정](#8️⃣-spring-security-설정)
10. [전체 동작 흐름](#📊-전체-동작-흐름)
11. [핵심 개념 정리](#🔑-핵심-개념-정리)

---

## 개요

카카오 OAuth 2.0 Authorization Code Grant Flow를 사용하여 소셜 로그인을 구현합니다.

### 주요 기능

- ✅ 카카오 OAuth 2.0 인증
- ✅ 자동 회원가입 (신규 사용자)
- ✅ 프로필 정보 업데이트 (기존 사용자)
- ✅ JWT 토큰 발급 (Access Token + Refresh Token)
- ✅ Refresh Token DB 저장
- ✅ 별도 계정 관리 (provider + providerId 유니크 키)

### 구현 파일 목록

- `src/main/java/com/example/myauth/entity/User.java` (수정)
- `src/main/resources/application.yaml` (수정)
- `src/main/java/com/example/myauth/config/KakaoOAuthProperties.java` (신규)
- `src/main/java/com/example/myauth/dto/kakao/KakaoOAuthDto.java` (신규)
- `src/main/java/com/example/myauth/service/KakaoOAuthService.java` (신규)
- `src/main/java/com/example/myauth/controller/KakaoAuthController.java` (신규)
- `src/main/java/com/example/myauth/repository/UserRepository.java` (수정)
- `src/main/java/com/example/myauth/config/SecurityConfig.java` (수정)

---

## 🔧 **카카오 개발자 콘솔 설정**

> ⚠️ **필수 작업**: 코드 구현 전에 카카오 개발자 콘솔에서 다음 설정을 완료해야 합니다.

### 1. 카카오 개발자 콘솔 접속

1. [카카오 개발자 콘솔](https://developers.kakao.com/) 접속
2. 로그인 후 **내 애플리케이션** 선택
3. 기존 앱을 선택하거나 **애플리케이션 추가하기** 클릭

### 2. 앱 키 확인

**앱 설정 > 앱 키** 메뉴에서 다음 정보 확인:

| 키 이름 | 설명 | application.yaml 매핑 |
|---------|------|------------------------|
| REST API 키 | OAuth 클라이언트 ID | `oauth.kakao.client-id` |

### 3. 플랫폼 설정 (Redirect URI 등록)

**앱 설정 > 플랫폼** 메뉴에서:

1. **Web 플랫폼 등록** 클릭
2. **사이트 도메인** 입력:
   - 개발: `http://localhost:9080`
   - 운영: 실제 도메인 (예: `https://myapp.com`)

### 4. ⚠️ **카카오 로그인 활성화 및 Redirect URI 등록**

**제품 설정 > 카카오 로그인** 메뉴에서:

1. **카카오 로그인 활성화**: `ON`
2. **Redirect URI 등록**:
   ```
   http://localhost:9080/auth/kakao/callback
   ```
   - **주의**: application.yaml의 `redirect-uri`와 정확히 일치해야 함
   - 운영 환경 추가 예시: `https://myapp.com/auth/kakao/callback`

### 5. ⚠️ **이메일 필수 동의 항목 설정 (중요!)**

> 🚨 **필수 작업**: 이 설정을 하지 않으면 이메일이 null이 되어 로그인 실패합니다.

**제품 설정 > 카카오 로그인 > 동의 항목** 메뉴에서:

1. **개인정보** 섹션에서 **카카오계정(이메일)** 항목 찾기
2. **설정** 버튼 클릭
3. 다음과 같이 설정:
   - **동의 단계**: `필수 동의` ⚠️
   - **수집 목적**: 사용자 식별 및 서비스 제공
   - **개인정보 보유 및 이용 기간**: 회원 탈퇴 시 또는 동의 철회 시
4. **저장** 클릭

**설정 예시:**
```
동의 항목: 카카오계정(이메일)
동의 단계: ✅ 필수 동의  ← 반드시 필수로 설정!
수집 목적: 사용자 식별 및 JWT 인증
```

### 6. Client Secret 활성화 (선택 사항)

**제품 설정 > 카카오 로그인 > 보안** 메뉴에서:

1. **Client Secret** > **코드 생성** 클릭
2. 생성된 코드를 application.yaml의 `client-secret`에 입력
3. **활성화 상태**: `ON`

> **참고**: Client Secret은 보안을 강화하지만 필수는 아닙니다.

### 7. 설정 확인 체크리스트

구현 전에 다음 항목들이 모두 완료되었는지 확인하세요:

- [ ] 카카오 로그인 활성화 (`ON`)
- [ ] Redirect URI 등록 (`http://localhost:9080/auth/kakao/callback`)
- [ ] **이메일 동의 항목을 "필수 동의"로 설정** ⚠️
- [ ] REST API 키 확인 및 application.yaml에 입력
- [ ] (선택) Client Secret 생성 및 활성화

### 설정 후 테스트 방법

1. 브라우저에서 `http://localhost:9080/auth/kakao/login` 접속
2. 카카오 로그인 페이지로 리다이렉트 확인
3. 카카오 계정으로 로그인
4. **이메일 제공 동의 화면이 표시되지 않고 자동 동의됨** (필수 동의 설정 완료)
5. JWT 토큰이 포함된 응답 확인

---

## 1️⃣ **데이터베이스 스키마 변경**

### User 엔티티 수정

**파일: `src/main/java/com/example/myauth/entity/User.java`**

#### 기존 필드 수정

```java
// ⚠️ 이메일 - 카카오 로그인에서 필수 동의 항목으로 설정 필요
@Column(nullable = false, unique = true, length = 100)
private String email;  // NOT NULL (카카오 개발자 콘솔에서 필수 동의 항목으로 설정)

// 비밀번호 - OAuth 로그인은 비밀번호 불필요
@Column(length = 255)
private String password;  // nullable = true (OAuth는 비밀번호 불필요)
```

#### 새로 추가된 OAuth 필드

```java
/**
 * OAuth 제공자 (LOCAL, KAKAO, GOOGLE 등)
 * 기본값: LOCAL (일반 회원가입)
 */
@Column(length = 20)
@ColumnDefault("'LOCAL'")
@Builder.Default
private String provider = "LOCAL";

/**
 * OAuth 제공자의 사용자 고유 ID
 * 카카오: 카카오 회원번호
 * 구글: 구글 사용자 ID
 */
@Column(name = "provider_id", length = 100)
private String providerId;

/**
 * 프로필 이미지 URL
 * OAuth 로그인 시 제공자로부터 받아옴
 */
@Column(name = "profile_image", length = 500)
private String profileImage;
```

### 데이터베이스 스키마 변경 SQL

```sql
-- ⚠️ email 컬럼을 NOT NULL로 변경 (카카오 개발자 콘솔에서 이메일 필수 동의 설정 필요)
ALTER TABLE users MODIFY COLUMN email VARCHAR(100) NOT NULL;

-- password 컬럼을 NULL 허용으로 변경 (OAuth 로그인은 비밀번호 불필요)
ALTER TABLE users MODIFY COLUMN password VARCHAR(255) NULL;

-- OAuth 필드 추가 (User 엔티티 수정 후 JPA가 자동 생성하거나 수동 실행)
ALTER TABLE users
ADD COLUMN provider VARCHAR(20) DEFAULT 'LOCAL' COMMENT 'OAuth 제공자',
ADD COLUMN provider_id VARCHAR(100) NULL COMMENT 'OAuth 제공자 사용자 고유 ID',
ADD COLUMN profile_image VARCHAR(500) NULL COMMENT '프로필 이미지 URL';

-- (provider, provider_id) 유니크 키 추가 (중복 가입 방지)
ALTER TABLE users
ADD UNIQUE KEY uk_provider_provider_id (provider, provider_id);
```

### 필드 설명

| 필드 | 타입 | NULL | 설명 |
|------|------|------|------|
| `provider` | VARCHAR(20) | NO | OAuth 제공자 (LOCAL, KAKAO, GOOGLE 등) |
| `providerId` | VARCHAR(100) | YES | OAuth 제공자의 사용자 고유 ID |
| `profileImage` | VARCHAR(500) | YES | 프로필 이미지 URL |

### 계정 구분 방식

| 로그인 방식 | provider | providerId | email | password |
|------------|----------|------------|-------|----------|
| 일반 회원가입 | `LOCAL` | `null` | 필수 | 필수 |
| 카카오 로그인 | `KAKAO` | 카카오 회원번호 | **필수** ⚠️ | `null` |
| 구글 로그인 | `GOOGLE` | 구글 사용자 ID | 필수 | `null` |

> ⚠️ **중요**: 카카오 로그인 시 이메일이 필수이므로, 카카오 개발자 콘솔에서 이메일을 **필수 동의 항목**으로 설정해야 합니다.

---

## 2️⃣ **설정 파일 관리**

### application.yaml 설정 추가

**파일: `src/main/resources/application.yaml`**

```yaml
oauth:
  kakao:
    # 카카오 개발자 콘솔의 REST API 키
    client-id: f0bfa98dfa477735feeb8dbfdfa1d105

    # 카카오 Client Secret (보안 설정에서 활성화 필요)
    client-secret: U4JgTAZGirCvVGmmnvuSlsoWlKFPstvV

    # 카카오 인증 후 돌아올 백엔드 URL
    redirect-uri: http://localhost:9080/auth/kakao/callback

    # 카카오 인가 코드 요청 URL
    authorization-uri: https://kauth.kakao.com/oauth/authorize

    # 카카오 토큰 요청 URL
    token-uri: https://kauth.kakao.com/oauth/token

    # 카카오 사용자 정보 조회 URL
    user-info-uri: https://kapi.kakao.com/v2/user/me
```

### KakaoOAuthProperties - 타입 안전한 설정 바인딩

**파일: `src/main/java/com/example/myauth/config/KakaoOAuthProperties.java`**

```java
package com.example.myauth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 카카오 OAuth 설정 Properties
 * application.yaml의 oauth.kakao 설정을 바인딩
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "oauth.kakao")
public class KakaoOAuthProperties {

  /** 카카오 REST API 키 */
  private String clientId;

  /** 카카오 Client Secret */
  private String clientSecret;

  /** 카카오 인증 후 리다이렉트 URI */
  private String redirectUri;

  /** 카카오 인가 코드 요청 URL */
  private String authorizationUri;

  /** 카카오 토큰 요청 URL */
  private String tokenUri;

  /** 카카오 사용자 정보 조회 URL */
  private String userInfoUri;
}
```

### 장점

- ✅ **타입 안전성**: 문자열 오타 방지
- ✅ **중앙 관리**: 설정 변경 시 한 곳만 수정
- ✅ **IDE 자동완성**: 필드명 자동완성 지원
- ✅ **Validation**: `@NotNull`, `@Pattern` 등으로 검증 가능

---

## 3️⃣ **DTO 구조 설계**

### KakaoOAuthDto - 카카오 API 응답 매핑

**파일: `src/main/java/com/example/myauth/dto/kakao/KakaoOAuthDto.java`**

```java
package com.example.myauth.dto.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 카카오 OAuth API 응답 DTO 모음
 */
public class KakaoOAuthDto {

  /**
   * 카카오 토큰 응답 DTO
   * POST https://kauth.kakao.com/oauth/token
   */
  @Getter
  @Setter
  @ToString
  public static class TokenResponse {
    /** 액세스 토큰 */
    @JsonProperty("access_token")
    private String accessToken;

    /** 토큰 타입 (Bearer) */
    @JsonProperty("token_type")
    private String tokenType;

    /** 리프레시 토큰 (optional) */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /** 액세스 토큰 만료 시간 (초) */
    @JsonProperty("expires_in")
    private Integer expiresIn;

    /** 리프레시 토큰 만료 시간 (초, optional) */
    @JsonProperty("refresh_token_expires_in")
    private Integer refreshTokenExpiresIn;

    /** 인증된 사용자의 정보 조회 권한 범위 (optional) */
    @JsonProperty("scope")
    private String scope;
  }

  /**
   * 카카오 사용자 정보 응답 DTO
   * GET https://kapi.kakao.com/v2/user/me
   */
  @Getter
  @Setter
  @ToString
  public static class UserInfoResponse {
    /** 회원번호 (고유 식별자) */
    @JsonProperty("id")
    private Long id;

    /** 서비스에 연결 완료된 시각 (UTC) */
    @JsonProperty("connected_at")
    private String connectedAt;

    /** 카카오계정 정보 */
    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    /** 프로필 정보 (deprecated, kakao_account.profile 사용 권장) */
    @JsonProperty("properties")
    private Properties properties;
  }

  /**
   * 카카오계정 정보
   */
  @Getter
  @Setter
  @ToString
  public static class KakaoAccount {
    /** 프로필 정보 */
    @JsonProperty("profile")
    private Profile profile;

    /** 이메일 */
    @JsonProperty("email")
    private String email;

    /** 이메일 제공 동의 여부 */
    @JsonProperty("has_email")
    private Boolean hasEmail;

    /** 이메일 인증 여부 */
    @JsonProperty("is_email_valid")
    private Boolean isEmailValid;

    /** 이메일 인증 여부 */
    @JsonProperty("is_email_verified")
    private Boolean isEmailVerified;

    /** 연령대 */
    @JsonProperty("age_range")
    private String ageRange;

    /** 생일 (MMDD) */
    @JsonProperty("birthday")
    private String birthday;

    /** 성별 (female/male) */
    @JsonProperty("gender")
    private String gender;
  }

  /**
   * 프로필 정보
   */
  @Getter
  @Setter
  @ToString
  public static class Profile {
    /** 닉네임 */
    @JsonProperty("nickname")
    private String nickname;

    /** 프로필 이미지 URL (640x640) */
    @JsonProperty("profile_image_url")
    private String profileImageUrl;

    /** 프로필 미리보기 이미지 URL (110x110) */
    @JsonProperty("thumbnail_image_url")
    private String thumbnailImageUrl;

    /** 프로필 이미지 URL 기본 이미지 여부 */
    @JsonProperty("is_default_image")
    private Boolean isDefaultImage;
  }

  /**
   * Properties (deprecated, 호환성 유지용)
   */
  @Getter
  @Setter
  @ToString
  public static class Properties {
    /** 닉네임 */
    @JsonProperty("nickname")
    private String nickname;

    /** 프로필 이미지 URL */
    @JsonProperty("profile_image")
    private String profileImage;

    /** 프로필 미리보기 이미지 URL */
    @JsonProperty("thumbnail_image")
    private String thumbnailImage;
  }
}
```

### Jackson의 @JsonProperty 사용 이유

| 카카오 API (snake_case) | Java 필드 (camelCase) |
|--------------------------|----------------------|
| `access_token` | `accessToken` |
| `profile_image_url` | `profileImageUrl` |
| `kakao_account` | `kakaoAccount` |

- 카카오 API는 **snake_case** 사용
- Java는 **camelCase** 사용
- `@JsonProperty`로 자동 변환 처리

---

## 4️⃣ **카카오 OAuth 서비스 로직**

### KakaoOAuthService - 핵심 비즈니스 로직

**파일: `src/main/java/com/example/myauth/service/KakaoOAuthService.java`**

```java
package com.example.myauth.service;

import com.example.myauth.config.KakaoOAuthProperties;
import com.example.myauth.dto.kakao.KakaoOAuthDto;
import com.example.myauth.dto.LoginResponse;
import com.example.myauth.entity.RefreshToken;
import com.example.myauth.entity.User;
import com.example.myauth.repository.RefreshTokenRepository;
import com.example.myauth.repository.UserRepository;
import com.example.myauth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * 카카오 OAuth 로그인 서비스
 * 카카오 API를 통한 소셜 로그인 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoOAuthService {

  private final KakaoOAuthProperties kakaoProperties;
  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final RestClient restClient = RestClient.create();

  /**
   * 카카오 인가 코드 요청 URL 생성
   * 사용자를 카카오 로그인 페이지로 리다이렉트하기 위한 URL
   *
   * @return 카카오 인가 코드 요청 URL
   */
  public String getAuthorizationUrl() {
    String url = UriComponentsBuilder
        .fromUriString(kakaoProperties.getAuthorizationUri())
        .queryParam("client_id", kakaoProperties.getClientId())
        .queryParam("redirect_uri", kakaoProperties.getRedirectUri())
        .queryParam("response_type", "code")
        .build()
        .toUriString();

    log.info("카카오 인가 URL 생성: {}", url);
    return url;
  }

  /**
   * Authorization Code로 카카오 Access Token 요청
   *
   * @param code 카카오 인가 코드
   * @return 카카오 토큰 응답 DTO
   */
  public KakaoOAuthDto.TokenResponse getAccessToken(String code) {
    log.info("카카오 Access Token 요청 시작 - code: {}", code);

    // 요청 파라미터 구성
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("grant_type", "authorization_code");
    params.add("client_id", kakaoProperties.getClientId());
    params.add("client_secret", kakaoProperties.getClientSecret());
    params.add("redirect_uri", kakaoProperties.getRedirectUri());
    params.add("code", code);

    // 카카오 토큰 API 호출
    KakaoOAuthDto.TokenResponse tokenResponse = restClient.post()
        .uri(kakaoProperties.getTokenUri())
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(params)
        .retrieve()
        .body(KakaoOAuthDto.TokenResponse.class);

    log.info("카카오 Access Token 발급 성공");
    return tokenResponse;
  }

  /**
   * 카카오 Access Token으로 사용자 정보 조회
   *
   * @param accessToken 카카오 액세스 토큰
   * @return 카카오 사용자 정보 응답 DTO
   */
  public KakaoOAuthDto.UserInfoResponse getUserInfo(String accessToken) {
    log.info("카카오 사용자 정보 조회 시작");

    // 카카오 사용자 정보 API 호출
    KakaoOAuthDto.UserInfoResponse userInfo = restClient.get()
        .uri(kakaoProperties.getUserInfoUri())
        .header("Authorization", "Bearer " + accessToken)
        .retrieve()
        .body(KakaoOAuthDto.UserInfoResponse.class);

    log.info("카카오 사용자 정보 조회 성공 - 카카오 ID: {}, 닉네임: {}",
        userInfo.getId(),
        userInfo.getKakaoAccount().getProfile().getNickname());

    return userInfo;
  }

  /**
   * 카카오 사용자 정보로 로그인 처리
   * 1. 기존 회원이면 로그인
   * 2. 신규 회원이면 자동 회원가입 후 로그인
   *
   * @param kakaoUserInfo 카카오 사용자 정보
   * @return 로그인 응답 DTO (JWT 포함)
   */
  @Transactional
  public LoginResponse processKakaoLogin(KakaoOAuthDto.UserInfoResponse kakaoUserInfo) {
    String providerId = String.valueOf(kakaoUserInfo.getId());
    String email = kakaoUserInfo.getKakaoAccount().getEmail();
    String nickname = kakaoUserInfo.getKakaoAccount().getProfile().getNickname();
    String profileImage = kakaoUserInfo.getKakaoAccount().getProfile().getProfileImageUrl();

    log.info("카카오 로그인 처리 시작 - 카카오 ID: {}, 이메일: {}, 닉네임: {}",
        providerId, email, nickname);

    // 1️⃣ 카카오 ID로 기존 회원 조회
    Optional<User> existingUser = userRepository.findByProviderAndProviderId("KAKAO", providerId);

    User user;
    if (existingUser.isPresent()) {
      // 기존 회원 - 로그인 처리
      user = existingUser.get();
      log.info("기존 카카오 회원 로그인: {}", user.getEmail());

      // 프로필 정보 업데이트 (닉네임, 프로필 이미지가 변경되었을 수 있음)
      user.setName(nickname);
      user.setProfileImage(profileImage);
      userRepository.save(user);

    } else {
      // 신규 회원 - 자동 회원가입
      log.info("신규 카카오 회원 가입 처리 - 이메일: {}, 닉네임: {}", email, nickname);

      user = User.builder()
          .email(email)  // 이메일이 없으면 null일 수 있음
          .name(nickname)
          .password(null)  // OAuth 로그인은 비밀번호 불필요
          .provider("KAKAO")
          .providerId(providerId)
          .profileImage(profileImage)
          .role(User.Role.ROLE_USER)
          .status(User.Status.ACTIVE)
          .isActive(true)
          .isSuperUser(false)
          .failedLoginAttempts(0)
          .build();

      userRepository.save(user);
      log.info("신규 카카오 회원 가입 완료 - ID: {}, 이메일: {}", user.getId(), user.getEmail());
    }

    // 2️⃣ JWT 토큰 생성
    String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getId());
    String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());
    log.info("JWT 토큰 생성 완료 - User ID: {}", user.getId());

    // 3️⃣ Refresh Token DB 저장
    RefreshToken refreshTokenEntity = RefreshToken.builder()
        .token(refreshToken)
        .user(user)
        .expiresAt(LocalDateTime.ofInstant(
            jwtTokenProvider.getRefreshTokenExpiryDate().toInstant(),
            ZoneId.systemDefault()
        ))
        .build();

    refreshTokenRepository.save(refreshTokenEntity);
    log.info("Refresh Token DB 저장 완료");

    // 4️⃣ 로그인 응답 생성
    LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
        .id(user.getId())
        .email(user.getEmail())
        .name(user.getName())
        .role(user.getRole().name())
        .build();

    return LoginResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .user(userInfo)
        .build();
  }
}
```

### 메서드별 설명

#### 1. getAuthorizationUrl()

**목적:** 카카오 로그인 페이지로 리다이렉트하기 위한 URL 생성

**생성 URL 예시:**
```
https://kauth.kakao.com/oauth/authorize?
  client_id=f0bfa98dfa477735feeb8dbfdfa1d105&
  redirect_uri=http://localhost:9080/auth/kakao/callback&
  response_type=code
```

#### 2. getAccessToken(String code)

**목적:** Authorization Code를 카카오 Access Token으로 교환

**요청:**
- Method: POST
- URL: `https://kauth.kakao.com/oauth/token`
- Content-Type: `application/x-www-form-urlencoded`
- Body: grant_type, client_id, client_secret, redirect_uri, code

**응답:**
```json
{
  "access_token": "...",
  "token_type": "bearer",
  "refresh_token": "...",
  "expires_in": 21599,
  "scope": "profile_nickname profile_image account_email"
}
```

#### 3. getUserInfo(String accessToken)

**목적:** 카카오 Access Token으로 사용자 정보 조회

**요청:**
- Method: GET
- URL: `https://kapi.kakao.com/v2/user/me`
- Header: `Authorization: Bearer {accessToken}`

**응답:**
```json
{
  "id": 3742819561,
  "kakao_account": {
    "profile": {
      "nickname": "홍길동",
      "profile_image_url": "https://..."
    },
    "email": "test@example.com"
  }
}
```

#### 4. processKakaoLogin(KakaoOAuthDto.UserInfoResponse kakaoUserInfo)

**목적:** 카카오 사용자 정보로 자동 회원가입/로그인 처리

**처리 로직:**

1. **기존 회원 조회**: `findByProviderAndProviderId("KAKAO", providerId)`
2. **기존 회원인 경우**:
   - 프로필 정보 업데이트 (닉네임, 프로필 이미지)
3. **신규 회원인 경우**:
   - User 엔티티 생성 (password = null, provider = "KAKAO")
   - DB 저장
4. **JWT 토큰 생성**:
   - Access Token (1시간 유효)
   - Refresh Token (7일 유효)
5. **Refresh Token DB 저장**
6. **LoginResponse 반환**

---

## 5️⃣ **컨트롤러 엔드포인트**

### KakaoAuthController

**파일: `src/main/java/com/example/myauth/controller/KakaoAuthController.java`**

```java
package com.example.myauth.controller;

import com.example.myauth.dto.kakao.KakaoOAuthDto;
import com.example.myauth.dto.ApiResponse;
import com.example.myauth.dto.LoginResponse;
import com.example.myauth.service.KakaoOAuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * 카카오 OAuth 로그인 컨트롤러
 * 카카오 소셜 로그인 엔드포인트를 제공
 */
@Slf4j
@RestController
@RequestMapping("/auth/kakao")
@RequiredArgsConstructor
public class KakaoAuthController {

  private final KakaoOAuthService kakaoOAuthService;

  /**
   * 카카오 로그인 시작
   * 사용자를 카카오 로그인 페이지로 리다이렉트
   *
   * GET /auth/kakao/login
   */
  @GetMapping("/login")
  public void kakaoLogin(HttpServletResponse response) throws IOException {
    log.info("카카오 로그인 요청");

    // 카카오 인가 코드 요청 URL 생성
    String authorizationUrl = kakaoOAuthService.getAuthorizationUrl();

    log.info("카카오 인가 페이지로 리다이렉트: {}", authorizationUrl);

    // 카카오 로그인 페이지로 리다이렉트
    response.sendRedirect(authorizationUrl);
  }

  /**
   * 카카오 로그인 콜백 처리
   * 카카오 인증 후 Authorization Code를 받아 JWT 발급
   *
   * GET /auth/kakao/callback?code=AUTHORIZATION_CODE
   *
   * @param code 카카오 인가 코드
   * @return 로그인 응답 (JWT 포함)
   */
  @GetMapping("/callback")
  public ResponseEntity<ApiResponse<LoginResponse>> kakaoCallback(@RequestParam String code) {
    log.info("카카오 로그인 콜백 - code: {}", code);

    try {
      // 1️⃣ Authorization Code로 카카오 Access Token 요청
      KakaoOAuthDto.TokenResponse tokenResponse = kakaoOAuthService.getAccessToken(code);
      log.info("카카오 Access Token 발급 완료");

      // 2️⃣ 카카오 Access Token으로 사용자 정보 조회
      KakaoOAuthDto.UserInfoResponse kakaoUserInfo = kakaoOAuthService.getUserInfo(tokenResponse.getAccessToken());
      log.info("카카오 사용자 정보 조회 완료 - 카카오 ID: {}", kakaoUserInfo.getId());

      // 3️⃣ 카카오 사용자 정보로 로그인 처리 (자동 회원가입 포함)
      LoginResponse loginResponse = kakaoOAuthService.processKakaoLogin(kakaoUserInfo);
      log.info("카카오 로그인 성공 - User ID: {}", loginResponse.getUser().getId());

      return ResponseEntity.ok(ApiResponse.success("카카오 로그인 성공", loginResponse));

    } catch (Exception e) {
      log.error("카카오 로그인 실패: {}", e.getMessage(), e);
      return ResponseEntity.status(500).body(ApiResponse.error("카카오 로그인 실패: " + e.getMessage()));
    }
  }
}
```

### 엔드포인트 설명

#### 1. GET /auth/kakao/login

**목적:** 카카오 로그인 시작

**요청:**
```http
GET http://localhost:9080/auth/kakao/login
```

**응답:**
```http
HTTP/1.1 302 Found
Location: https://kauth.kakao.com/oauth/authorize?client_id=...&redirect_uri=...&response_type=code
```

**동작:**
1. 카카오 인가 URL 생성
2. HTTP 302 리다이렉트로 카카오 로그인 페이지로 이동
3. 사용자가 카카오에서 로그인 및 동의

---

#### 2. GET /auth/kakao/callback

**목적:** 카카오 인증 후 콜백 처리

**요청:**
```http
GET http://localhost:9080/auth/kakao/callback?code=AUTHORIZATION_CODE
```

**응답:**
```json
{
  "success": true,
  "message": "카카오 로그인 성공",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "user": {
      "id": 42,
      "email": "test@example.com",
      "name": "홍길동",
      "role": "ROLE_USER"
    }
  }
}
```

**처리 흐름:**
1. `code` 파라미터로 카카오 Access Token 요청
2. Access Token으로 사용자 정보 조회
3. 자동 회원가입/로그인 처리
4. JWT 토큰 발급 및 반환

---

## 6️⃣ **카카오 로그아웃**

### 현재 구현 방식

현재 구현에서는 **애플리케이션 레벨 로그아웃만** 처리합니다. 카카오 Access Token을 별도로 저장하지 않으므로, 카카오 서버에 로그아웃 API를 호출하지 않습니다.

### 로그아웃 동작 방식

#### 카카오 로그인 사용자도 기존 `/logout` 엔드포인트 사용

카카오 OAuth로 로그인한 사용자도 일반 회원가입 사용자와 동일하게 `/logout` 엔드포인트를 사용합니다.

**로그아웃 요청:**
```http
POST http://localhost:9080/logout
Authorization: Bearer {JWT_ACCESS_TOKEN}
```

**처리 과정:**
1. ✅ 우리 서비스의 JWT Refresh Token DB에서 삭제
2. ✅ 사용자 세션 종료 (애플리케이션 레벨)
3. ⚠️ **카카오 서버에는 로그아웃 요청을 보내지 않음**

**결과:**
```json
{
  "success": true,
  "message": "로그아웃 성공"
}
```

### 로그아웃 후 상태

| 항목 | 상태 | 설명 |
|------|------|------|
| 우리 서비스 JWT | ❌ 만료 | Refresh Token이 DB에서 삭제되어 재발급 불가 |
| 카카오 세션 | ✅ 유지 | 카카오 서버에는 여전히 로그인 상태 |
| 재로그인 시 | 자동 로그인 | 동의 화면 없이 바로 로그인됨 |

### 재로그인 시 동작

사용자가 로그아웃 후 다시 카카오 로그인을 시도하면:

1. `/auth/kakao/login` 접속
2. 카카오 로그인 페이지로 리다이렉트
3. **카카오 서버가 이미 로그인 상태임을 확인**
4. **동의 화면 없이 바로 Authorization Code 발급** ⚠️
5. 콜백으로 돌아와 JWT 재발급
6. 로그인 완료

> ⚠️ **중요**: 카카오 서버 세션이 유지되므로, 사용자는 별도의 로그인/동의 과정 없이 자동으로 로그인됩니다.

### 완전한 로그아웃이 필요한 경우

만약 카카오 서버에서도 완전히 로그아웃 처리가 필요하다면:

#### 옵션 1: 카카오 Access Token 저장 방식

**필요한 변경사항:**

1. **User 테이블에 카카오 토큰 필드 추가**
```sql
ALTER TABLE users
ADD COLUMN kakao_access_token VARCHAR(500) NULL COMMENT '카카오 Access Token',
ADD COLUMN kakao_refresh_token VARCHAR(500) NULL COMMENT '카카오 Refresh Token',
ADD COLUMN kakao_token_expires_at DATETIME NULL COMMENT '카카오 Access Token 만료 시간';
```

2. **KakaoOAuthService에 로그아웃 메서드 추가**
```java
public void logoutFromKakao(String kakaoAccessToken) {
  restClient.post()
    .uri("https://kapi.kakao.com/v1/user/logout")
    .header("Authorization", "Bearer " + kakaoAccessToken)
    .retrieve()
    .body(KakaoOAuthDto.LogoutResponse.class);
}
```

3. **CustomLogoutHandler에서 카카오 로그아웃 호출**
```java
if (user.getProvider().equals("KAKAO") && user.getKakaoAccessToken() != null) {
  kakaoOAuthService.logoutFromKakao(user.getKakaoAccessToken());
}
```

#### 옵션 2: 프론트엔드에서 카카오 로그아웃 처리

카카오 JavaScript SDK를 사용하여 프론트엔드에서 직접 카카오 로그아웃 처리:

```javascript
// 프론트엔드 (JavaScript)
// 1. 우리 서비스 로그아웃
await fetch('/logout', {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${accessToken}` }
});

// 2. 카카오 로그아웃
if (Kakao.Auth.getAccessToken()) {
  Kakao.Auth.logout(() => {
    console.log('카카오 로그아웃 완료');
  });
}
```

### 현재 방식의 장단점

#### 장점
- ✅ 구현이 간단함
- ✅ 카카오 Access Token 관리 불필요
- ✅ 보안 리스크 감소 (토큰 저장 안 함)
- ✅ 사용자 편의성 향상 (재로그인 시 빠른 인증)

#### 단점
- ⚠️ 카카오 서버 세션이 유지됨
- ⚠️ 완전한 로그아웃이 아님
- ⚠️ 공용 PC에서 보안 이슈 가능 (다음 사용자가 자동 로그인됨)

### 사용 시나리오별 권장사항

| 시나리오 | 권장 방식 | 이유 |
|---------|----------|------|
| 개인 기기 (모바일, PC) | 현재 방식 | 사용자 편의성 우선 |
| 공용 PC (PC방, 도서관) | 카카오 로그아웃 추가 | 보안 우선 |
| 테스트 환경 | 현재 방식 | 개발 편의성 |
| 프라이버시 중시 서비스 | 카카오 로그아웃 추가 | 완전한 로그아웃 필요 |

### 참고: 카카오 로그아웃 vs 연결 끊기

| 구분 | 로그아웃 API | 연결 끊기 API |
|------|-------------|--------------|
| API | `POST /v1/user/logout` | `POST /v1/user/unlink` |
| 효과 | 카카오 Access Token 만료 | 카카오 계정 연동 해제 |
| 재로그인 | 동의 없이 재로그인 가능 | 처음부터 다시 동의 필요 |
| 사용 시기 | 일반 로그아웃 | 회원 탈퇴 시 |
| DB 데이터 | 유지 | 삭제 권장 |

---

## 7️⃣ **Repository 메서드 추가**

### UserRepository

**파일: `src/main/java/com/example/myauth/repository/UserRepository.java`**

```java
package com.example.myauth.repository;

import com.example.myauth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 정보를 관리하는 Repository
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * 이메일로 사용자를 조회한다
   */
  Optional<User> findByEmail(String email);

  /**
   * 이메일이 이미 존재하는지 확인한다
   */
  boolean existsByEmail(String email);

  /**
   * OAuth 제공자와 제공자 ID로 사용자를 조회한다 (카카오, 구글 등)
   *
   * @param provider OAuth 제공자 (KAKAO, GOOGLE 등)
   * @param providerId OAuth 제공자의 사용자 고유 ID
   * @return 사용자 정보 (Optional)
   */
  Optional<User> findByProviderAndProviderId(String provider, String providerId);
}
```

### 메서드 설명

#### findByProviderAndProviderId(String provider, String providerId)

**목적:** OAuth 로그인 시 기존 회원 조회

**사용 예시:**
```java
// 카카오 회원 조회
Optional<User> user = userRepository.findByProviderAndProviderId("KAKAO", "3742819561");

// 구글 회원 조회
Optional<User> user = userRepository.findByProviderAndProviderId("GOOGLE", "105123456789");
```

**생성되는 SQL:**
```sql
SELECT * FROM users
WHERE provider = ? AND provider_id = ?
```

**중요:**
- `(provider, providerId)` 조합은 DB에서 유니크 키로 설정
- 중복 가입 방지
- Spring Data JPA가 메서드명으로 자동 쿼리 생성

---

## 8️⃣ **Spring Security 설정**

### SecurityConfig

**파일: `src/main/java/com/example/myauth/config/SecurityConfig.java`**

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
  http
      // ... 기존 설정 ...

      // 경로별 인증 규칙 설정
      .authorizeHttpRequests(auth ->
          auth
              // 인증 없이 접근 가능한 경로
              .requestMatchers("/health", "/signup", "/login", "/loginEx", "/refresh").permitAll()

              // ✅ 카카오 OAuth 로그인 경로 (인증 불필요)
              .requestMatchers("/auth/kakao/**").permitAll()

              // 그 외 모든 요청은 인증 필요
              .anyRequest().authenticated()
      )

      // ... 기존 설정 ...

  return http.build();
}
```

### 설정 이유

| 경로 | 인증 필요 여부 | 이유 |
|------|--------------|------|
| `/auth/kakao/login` | ❌ 불필요 | 로그인 시작점 - 사용자를 카카오로 리다이렉트 |
| `/auth/kakao/callback` | ❌ 불필요 | 카카오에서 돌아오는 콜백 - 아직 인증 전 |

**permitAll() 필요 이유:**
- OAuth 로그인은 인증 전에 접근해야 하는 엔드포인트
- `/auth/kakao/**` 전체를 허용하여 향후 확장 가능

---

## 📊 **전체 동작 흐름**

### Sequence Diagram

```
사용자                  백엔드                    카카오
  │                      │                        │
  │─────(1) GET /auth/kakao/login─────>│         │
  │                      │                        │
  │                      │─(2) 인가 URL 생성      │
  │                      │                        │
  │<────(3) 302 Redirect────────────────│         │
  │                      │                        │
  │─────(4) 카카오 로그인 페이지 접속──────────────>│
  │                      │                        │
  │<────(5) 로그인 & 동의 화면─────────────────────│
  │                      │                        │
  │─────(6) 로그인 & 동의─────────────────────────>│
  │                      │                        │
  │<────(7) 302 Redirect + code────────────────────│
  │   Location: http://localhost:9080/auth/kakao/callback?code=XXX
  │                      │                        │
  │─────(8) GET /auth/kakao/callback?code=XXX─>│  │
  │                      │                        │
  │                      │─(9) POST /oauth/token ->│
  │                      │   (code로 토큰 요청)     │
  │                      │                        │
  │                      │<─(10) Access Token─────│
  │                      │                        │
  │                      │─(11) GET /v2/user/me ->│
  │                      │   (Bearer Token)       │
  │                      │                        │
  │                      │<─(12) 사용자 정보──────│
  │                      │   (ID, 닉네임, 이메일)  │
  │                      │                        │
  │                      │─(13) DB 조회/저장      │
  │                      │   - findByProviderAndProviderId
  │                      │   - save (신규 회원 시)
  │                      │                        │
  │                      │─(14) JWT 생성          │
  │                      │   - Access Token       │
  │                      │   - Refresh Token      │
  │                      │                        │
  │<────(15) 200 OK + JWT 토큰─────────────────────│
  │   {                  │                        │
  │     "accessToken": "...",                     │
  │     "refreshToken": "...",                    │
  │     "user": { ... }  │                        │
  │   }                  │                        │
```

### 단계별 설명

| 단계 | 설명 | 담당 |
|------|------|------|
| 1 | 사용자가 카카오 로그인 시작 | 사용자 |
| 2 | 카카오 인가 URL 생성 | 백엔드 |
| 3 | 카카오 로그인 페이지로 리다이렉트 | 백엔드 |
| 4-6 | 사용자가 카카오에서 로그인 및 동의 | 사용자 + 카카오 |
| 7 | Authorization Code와 함께 백엔드로 리다이렉트 | 카카오 |
| 8 | 백엔드 콜백 엔드포인트 호출 | 사용자 |
| 9-10 | Authorization Code로 Access Token 요청 | 백엔드 + 카카오 |
| 11-12 | Access Token으로 사용자 정보 조회 | 백엔드 + 카카오 |
| 13 | DB에서 회원 조회/저장 | 백엔드 |
| 14 | JWT 토큰 생성 | 백엔드 |
| 15 | JWT 토큰 반환 | 백엔드 |

---

## 🔑 **핵심 개념 정리**

### OAuth 2.0 Authorization Code Grant Flow

**왜 Authorization Code Flow를 사용하는가?**

1. **보안성**: Client Secret이 프론트엔드에 노출되지 않음
2. **두 단계 인증**:
   - Authorization Code 발급 (프론트엔드 노출 가능)
   - Access Token 교환 (백엔드에서만 처리)

### 보안 특징

| 항목 | 노출 여부 | 이유 |
|------|----------|------|
| Authorization Code | ✅ 프론트엔드 노출 가능 | 일회용, 짧은 유효시간 (10분) |
| Access Token | ❌ 백엔드에서만 사용 | 민감 정보 접근 가능 |
| Client Secret | ❌ 백엔드에 보관 | 절대 프론트엔드 노출 금지 |

### 별도 계정 관리 전략

**왜 별도 계정으로 관리하는가?**

- 같은 이메일이라도 provider가 다르면 별도 계정
- 추후 계정 연동 기능으로 통합 가능
- 보안: OAuth 계정과 일반 계정의 인증 방식이 다름

**계정 구분 예시:**

| 사용자 | provider | providerId | email | 별도 계정? |
|--------|----------|------------|-------|----------|
| 홍길동 | LOCAL | null | hong@example.com | - |
| 홍길동 | KAKAO | 3742819561 | hong@example.com | ✅ 별도 |
| 홍길동 | GOOGLE | 105123456789 | hong@example.com | ✅ 별도 |

### RestClient vs RestTemplate

**Spring 6부터 RestClient 사용 권장:**

```java
// RestClient (Spring 6+)
RestClient restClient = RestClient.create();

restClient.get()
    .uri("https://api.example.com/data")
    .header("Authorization", "Bearer token")
    .retrieve()
    .body(ResponseDto.class);

// RestTemplate (Legacy)
RestTemplate restTemplate = new RestTemplate();
HttpHeaders headers = new HttpHeaders();
headers.set("Authorization", "Bearer token");
HttpEntity<Void> entity = new HttpEntity<>(headers);
ResponseDto response = restTemplate.exchange(
    "https://api.example.com/data",
    HttpMethod.GET,
    entity,
    ResponseDto.class
).getBody();
```

**RestClient의 장점:**
- ✅ 더 간결한 API
- ✅ Fluent API 스타일
- ✅ Spring 6+ 공식 권장
- ✅ 함수형 프로그래밍 지원

### JWT vs OAuth Token

**카카오 OAuth Token과 우리 서비스 JWT의 차이:**

| 구분 | 카카오 Access Token | 우리 서비스 JWT |
|------|-------------------|----------------|
| 용도 | 카카오 API 호출용 | 우리 서비스 인증용 |
| 발급자 | 카카오 | 우리 백엔드 |
| 저장 위치 | 백엔드에서 임시 사용 | 프론트엔드 저장 |
| 유효기간 | 카카오 정책 (6시간) | 우리 정책 (1시간) |

**흐름:**
```
카카오 Access Token (카카오 API 호출용)
         ↓
    사용자 정보 조회
         ↓
우리 서비스 JWT 발급 (우리 서비스 인증용)
         ↓
   프론트엔드 저장
```

---

## 🧪 **테스트 방법**

### 1. 브라우저 테스트

```
1. 브라우저에서 http://localhost:9080/auth/kakao/login 접속
2. 카카오 로그인 페이지로 리다이렉트
3. 카카오 계정으로 로그인 및 동의
4. 자동으로 /auth/kakao/callback 호출
5. JWT 토큰 응답 확인
```

### 2. cURL 테스트

```bash
# 1. 카카오 로그인 URL 확인
curl -v http://localhost:9080/auth/kakao/login

# 2. 콜백 테스트 (실제 code는 카카오에서 발급받아야 함)
curl -v "http://localhost:9080/auth/kakao/callback?code=AUTHORIZATION_CODE"
```

### 3. Postman 테스트

**Step 1: 카카오 로그인**
```
GET http://localhost:9080/auth/kakao/login
```
→ Location 헤더의 URL을 브라우저에서 열어 로그인

**Step 2: 콜백 처리**
```
GET http://localhost:9080/auth/kakao/callback?code={카카오에서 받은 코드}
```

---

## 🔧 **트러블슈팅**

### 문제 1: password 필드 NOT NULL 에러

**에러 메시지:**
```
Field 'password' doesn't have a default value
```

**원인:**
- DB 스키마에서 password 컬럼이 NOT NULL로 설정됨
- OAuth 로그인은 password가 null이어야 함

**해결:**
```sql
ALTER TABLE users MODIFY COLUMN password VARCHAR(255) NULL;
```

---

### 문제 2: 카카오 리다이렉트 URI 불일치

**에러 메시지:**
```
KOE320: invalid redirect_uri
```

**원인:**
- 카카오 개발자 콘솔의 Redirect URI와 불일치

**해결:**
1. 카카오 개발자 콘솔 접속
2. 내 애플리케이션 > 앱 설정 > 플랫폼
3. Redirect URI 등록: `http://localhost:9080/auth/kakao/callback`

---

### 문제 3: Client Secret 불일치

**에러 메시지:**
```
KOE303: client_secret mismatch
```

**원인:**
- application.yaml의 client-secret이 잘못됨

**해결:**
1. 카카오 개발자 콘솔 > 보안 탭
2. Client Secret 확인
3. application.yaml 업데이트

---

## 📚 **참고 자료**

### 카카오 공식 문서

- [카카오 로그인 개요](https://developers.kakao.com/docs/latest/ko/kakaologin/common)
- [카카오 로그인 REST API](https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api)
- [사용자 정보 가져오기](https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api#req-user-info)

### Spring 공식 문서

- [Spring Security OAuth 2.0](https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html)
- [RestClient Documentation](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-restclient)
- [@ConfigurationProperties](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config.typesafe-configuration-properties)

---

## 📝 **다음 단계 (선택사항)**

### 1. 구글 OAuth 추가

- GoogleOAuthProperties 생성
- GoogleOAuthService 구현
- GoogleAuthController 추가
- User 엔티티의 provider 필드 활용

### 2. 계정 연동 기능

- 일반 계정과 OAuth 계정 연동
- `linkOAuthAccount()` 메서드 구현
- 같은 이메일의 계정들을 하나로 통합

### 3. OAuth 토큰 갱신

- 카카오 Refresh Token 저장
- Access Token 만료 시 자동 갱신
- 장기 로그인 유지

### 4. 회원 탈퇴 시 카카오 연결 해제

- 카카오 연결 끊기 API 호출
- User 삭제 시 카카오에도 통보

---

## ✅ **체크리스트**

구현 완료 항목:

- [x] User 엔티티에 OAuth 필드 추가
- [x] application.yaml에 카카오 설정 추가
- [x] KakaoOAuthProperties 생성
- [x] KakaoOAuthDto 생성
- [x] KakaoOAuthService 구현
- [x] KakaoAuthController 구현
- [x] UserRepository에 findByProviderAndProviderId 추가
- [x] SecurityConfig에 /auth/kakao/** 허용
- [x] 데이터베이스 스키마 수정
- [x] 카카오 로그인 테스트 완료

---

## 📞 **문의 및 지원**

구현 중 문제가 발생하면:

1. 서버 로그 확인 (`log.info`, `log.error` 출력)
2. 카카오 개발자 콘솔의 오류 메시지 확인
3. DB 스키마가 올바른지 확인 (`DESCRIBE users;`)
4. application.yaml 설정값 재확인

---

**문서 작성일:** 2025-11-30
**구현 버전:** Spring Boot 4.0.0, Spring Security 7.0.0
**작성자:** Claude Code
