package com.example.myauth.service;

import com.example.myauth.dto.ApiResponse;
import com.example.myauth.dto.LoginRequest;
import com.example.myauth.dto.LoginResponse;
import com.example.myauth.dto.SignupRequest;
import com.example.myauth.dto.TokenRefreshResponse;
import com.example.myauth.entity.RefreshToken;
import com.example.myauth.entity.User;
import com.example.myauth.repository.RefreshTokenRepository;
import com.example.myauth.repository.UserRepository;
import com.example.myauth.security.CustomUserDetails;
import com.example.myauth.security.CustomUserDetailsService;
import com.example.myauth.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenRepository refreshTokenRepository;
  private final CustomUserDetailsService customUserDetailsService;


  /**
   * 회원가입 처리
   * noRollbackFor: DataIntegrityViolationException이 발생해도 rollback하지 않음
   * → catch에서 에러 응답을 반환하므로 트랜잭션을 정상 종료해야 함
   */
  @Transactional(noRollbackFor = DataIntegrityViolationException.class)
  public ApiResponse<Void> registerUser(SignupRequest signupRequest) {
    // 이메일을 정규화한다 (공백 제거, 소문자 변환)
    String normalizedEmail = signupRequest.getEmail().trim().toLowerCase();

    try {
      // signupRequest 정보를 이용하여 User Entity 인스턴스를 생성한다
      User user = User.builder()
          .email(normalizedEmail)  // 정규화된 이메일 사용
          .password(passwordEncoder.encode(signupRequest.getPassword()))
          .name(signupRequest.getUsername())
          .role(User.Role.ROLE_USER)
          .status(User.Status.ACTIVE)
          .isActive(true)
          .build();

      // DB에 저장한다 - unique constraint 위반 시 예외 발생
      userRepository.save(user);
      log.info("회원 가입 성공 : {}", user.getEmail());

      return ApiResponse.success("회원가입이 완료되었습니다.");

    } catch (DataIntegrityViolationException e) {
      // unique constraint 위반 (이미 존재하는 이메일)
      log.warn("중복된 이메일로 가입 시도 : {}", normalizedEmail);
      return ApiResponse.error("이미 가입된 이메일입니다.");

    } catch (Exception e) {
      // 기타 예외 처리
      log.error("회원가입 중 오류 발생 : {}", e.getMessage(), e);
      return ApiResponse.error("회원가입 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
    }
  }

  @Transactional
  public LoginResponse login(@Valid LoginRequest loginRequest) {
    // 1️⃣ 이메일을 정규화한다 (회원가입과 동일하게 처리)
    String normalizedEmail = loginRequest.getEmail().trim().toLowerCase();
    log.info("로그인 시도: {}", normalizedEmail);

    // 2️⃣ 사용자를 조회한다
    User user = userRepository.findByEmail(normalizedEmail)
        .orElse(null);

    // 3️⃣ 사용자가 존재하지 않으면 에러 반환
    if (user == null) {
      log.warn("존재하지 않는 이메일로 로그인 시도: {}", normalizedEmail);
      // 보안상 이유로 이메일이 틀렸는지 비밀번호가 틀렸는지 알려주지 않음
      return createErrorResponse("이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    // 4️⃣ 비밀번호를 검증한다
    boolean isPasswordValid = passwordEncoder.matches(
        loginRequest.getPassword(),  // 입력된 평문 비밀번호
        user.getPassword()            // DB에 저장된 암호화된 비밀번호
    );

    if (!isPasswordValid) {
      log.warn("잘못된 비밀번호로 로그인 시도: {}", normalizedEmail);
      // 보안상 동일한 에러 메시지 사용
      return createErrorResponse("이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    // 5️⃣ 계정 상태를 확인한다

    // 5-1. 활성화 여부 확인
    if (!user.getIsActive()) {
      log.warn("비활성화된 계정으로 로그인 시도: {}", normalizedEmail);
      return createErrorResponse("비활성화된 계정입니다. 고객센터에 문의해주세요.");
    }

    // 5-2. 계정 상태 확인
    if (user.getStatus() != User.Status.ACTIVE) {
      log.warn("비정상 상태 계정으로 로그인 시도: {} (상태: {})", normalizedEmail, user.getStatus());

      // 상태에 따라 다른 메시지 반환
      String errorMessage = switch (user.getStatus()) {
        case SUSPENDED -> "정지된 계정입니다. 고객센터에 문의해주세요.";
        case DELETED -> "삭제된 계정입니다.";
        case INACTIVE -> "비활성화된 계정입니다. 고객센터에 문의해주세요.";
        case PENDING_VERIFICATION -> "이메일 인증이 필요합니다.";
        default -> "로그인할 수 없는 계정 상태입니다.";
      };
      return createErrorResponse(errorMessage);
    }

//    // 5-3. 계정 잠금 확인
//    if (user.getAccountLockedUntil() != null &&
//        user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
//      log.warn("잠긴 계정으로 로그인 시도: {} (잠금 해제: {})",
//          normalizedEmail, user.getAccountLockedUntil());
//      return createErrorResponse(
//          String.format("계정이 잠겨있습니다. %s 이후 다시 시도해주세요.",
//              user.getAccountLockedUntil())
//      );
//    }

    // 6️⃣ JWT 토큰 생성
    String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getId());
    String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

    log.info("JWT 토큰 생성 완료: {}", normalizedEmail);

    // 7️⃣ Refresh Token을 DB에 저장
    RefreshToken refreshTokenEntity = RefreshToken.builder()
        .token(refreshToken)
        .user(user)
        .expiresAt(LocalDateTime.ofInstant(
            jwtTokenProvider.getRefreshTokenExpiryDate().toInstant(),
            ZoneId.systemDefault()
        ))
        .build();

    refreshTokenRepository.save(refreshTokenEntity);
    log.info("Refresh Token DB 저장 완료: {}", normalizedEmail);

    // 8️⃣ 로그인 성공 응답 반환
    LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
        .id(user.getId())
        .email(user.getEmail())
        .name(user.getName())
        .role(user.getRole().name())
        .build();

    log.info("로그인 성공: {}", normalizedEmail);
    return LoginResponse.success(accessToken, refreshToken, userInfo);
  }

  /**
   * 에러 응답 생성 헬퍼 메서드
   */
  private LoginResponse createErrorResponse(String message) {
    return LoginResponse.builder()
        .success(false)
        .message(message)
        .build();
  }

  /**
   * 로그인 처리 (CustomUserDetailsService 사용 버전)
   *
   * 이 메서드는 Spring Security의 표준 방식(UserDetailsService)을 사용하여 로그인을 처리한다
   * 기존 login() 메서드와의 차이점:
   * - UserDetailsService를 통해 사용자 정보를 로드 (표준 방식)
   * - 코드 재사용성이 높고 Spring Security와의 통합이 좋음
   * - 나중에 Session 기반 인증으로 전환하기 쉬움
   *
   * @param loginRequest 로그인 요청 정보 (email, password)
   * @return 로그인 응답 (성공 시 토큰 포함, 실패 시 에러 메시지)
   */
  @Transactional
  public LoginResponse loginEx(@Valid LoginRequest loginRequest) {
    // 1️⃣ 이메일을 정규화한다
    String normalizedEmail = loginRequest.getEmail().trim().toLowerCase();
    log.info("로그인 시도 (loginEx): {}", normalizedEmail);

    try {
      // 2️⃣ CustomUserDetailsService를 통해 사용자 정보를 로드
      // Spring Security의 표준 방식으로 사용자 조회
      UserDetails userDetails = customUserDetailsService.loadUserByUsername(normalizedEmail);

      // 3️⃣ UserDetails가 null이면 사용자가 존재하지 않음
      if (userDetails == null) {
        log.warn("존재하지 않는 이메일로 로그인 시도 (loginEx): {}", normalizedEmail);
        return createErrorResponse("이메일 또는 비밀번호가 올바르지 않습니다.");
      }

      // 4️⃣ 비밀번호를 검증한다
      boolean isPasswordValid = passwordEncoder.matches(
          loginRequest.getPassword(),        // 입력된 평문 비밀번호
          userDetails.getPassword()          // DB에 저장된 암호화된 비밀번호
      );

      if (!isPasswordValid) {
        log.warn("잘못된 비밀번호로 로그인 시도 (loginEx): {}", normalizedEmail);
        return createErrorResponse("이메일 또는 비밀번호가 올바르지 않습니다.");
      }

      // 5️⃣ 계정 상태를 확인한다
      // UserDetails 인터페이스의 메서드들을 사용하여 계정 상태 확인
      if (!userDetails.isEnabled()) {
        log.warn("비활성화된 계정으로 로그인 시도 (loginEx): {}", normalizedEmail);
        return createErrorResponse("비활성화된 계정입니다. 고객센터에 문의해주세요.");
      }

      if (!userDetails.isAccountNonLocked()) {
        log.warn("잠긴 계정으로 로그인 시도 (loginEx): {}", normalizedEmail);
        return createErrorResponse("잠긴 계정입니다. 고객센터에 문의해주세요.");
      }

      // 6️⃣ CustomUserDetails에서 User 엔티티 추출
      // CustomUserDetails는 우리가 만든 클래스이므로 캐스팅 가능
      CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
      User user = customUserDetails.getUser();

      // 7️⃣ JWT 토큰 생성
      String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getId());
      String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

      log.info("JWT 토큰 생성 완료 (loginEx): {}", normalizedEmail);

      // 8️⃣ Refresh Token을 DB에 저장
      RefreshToken refreshTokenEntity = RefreshToken.builder()
          .token(refreshToken)
          .user(user)
          .expiresAt(LocalDateTime.ofInstant(
              jwtTokenProvider.getRefreshTokenExpiryDate().toInstant(),
              ZoneId.systemDefault()
          ))
          .build();

      refreshTokenRepository.save(refreshTokenEntity);
      log.info("Refresh Token DB 저장 완료 (loginEx): {}", normalizedEmail);

      // 9️⃣ 로그인 성공 응답 반환
      LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
          .id(user.getId())
          .email(user.getEmail())
          .name(user.getName())
          .role(user.getRole().name())
          .build();

      log.info("로그인 성공 (loginEx): {}", normalizedEmail);
      return LoginResponse.success(accessToken, refreshToken, userInfo);

    } catch (Exception e) {
      // 예외 발생 시 에러 응답
      log.error("로그인 처리 중 오류 발생 (loginEx): {}", e.getMessage(), e);
      return createErrorResponse("로그인 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
    }
  }

  /**
   * Refresh Token으로 Access Token 갱신
   *
   * Refresh Token의 유효성을 검증하고, 새로운 Access Token을 발급한다
   * - Refresh Token이 유효하면 새 Access Token 생성
   * - Refresh Token이 만료되었거나 유효하지 않으면 에러 반환
   *
   * @param refreshToken Refresh Token
   * @return TokenRefreshResponse (성공 시 새 Access Token 포함)
   */
  @Transactional(readOnly = true)
  public TokenRefreshResponse refreshAccessToken(String refreshToken) {
    log.info("Access Token 갱신 요청");

    try {
      // 1️⃣ Refresh Token 검증
      if (!jwtTokenProvider.validateToken(refreshToken)) {
        log.warn("유효하지 않은 Refresh Token");
        return TokenRefreshResponse.error("유효하지 않은 Refresh Token입니다. 다시 로그인해주세요.");
      }

      // 2️⃣ Refresh Token에서 이메일 추출
      String email = jwtTokenProvider.getEmailFromToken(refreshToken);
      log.debug("Refresh Token에서 추출한 이메일: {}", email);

      // 3️⃣ DB에 해당 Refresh Token이 존재하는지 확인
      RefreshToken refreshTokenEntity = refreshTokenRepository.findByToken(refreshToken)
          .orElse(null);

      if (refreshTokenEntity == null) {
        log.warn("DB에 존재하지 않는 Refresh Token");
        return TokenRefreshResponse.error("유효하지 않은 Refresh Token입니다. 다시 로그인해주세요.");
      }

      // 4️⃣ Refresh Token이 만료되었는지 확인
      if (refreshTokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
        log.warn("만료된 Refresh Token: {}", email);
        return TokenRefreshResponse.error("Refresh Token이 만료되었습니다. 다시 로그인해주세요.");
      }

      // 5️⃣ 사용자 조회
      User user = refreshTokenEntity.getUser();
      if (user == null || !user.getIsActive() || user.getStatus() != User.Status.ACTIVE) {
        log.warn("비활성화된 사용자: {}", email);
        return TokenRefreshResponse.error("비활성화된 계정입니다. 고객센터에 문의해주세요.");
      }

      // 6️⃣ 새 Access Token 생성
      String newAccessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getId());
      log.info("새 Access Token 발급 성공: {}", email);

      return TokenRefreshResponse.success(newAccessToken);

    } catch (Exception e) {
      log.error("Access Token 갱신 중 오류 발생: {}", e.getMessage(), e);
      return TokenRefreshResponse.error("Access Token 갱신 중 오류가 발생했습니다. 다시 로그인해주세요.");
    }
  }
}
