package com.example.myauth.security;

import com.example.myauth.dto.JwtErrorResponse;
import com.example.myauth.entity.User;
import com.example.myauth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 인증 필터
 * 모든 HTTP 요청에 대해 JWT 토큰을 검증하고 인증 정보를 SecurityContext에 설정한다
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;

  /**
   * 모든 HTTP 요청마다 실행되는 필터 메서드
   * JWT 토큰을 검증하고 만료/유효하지 않은 토큰을 구분하여 처리
   */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

    try {
      // 1️⃣ Authorization 헤더에서 JWT 토큰 추출
      String token = extractTokenFromRequest(request);

      // 2️⃣ 토큰이 존재하는지 확인
      if (token != null) {
        try {
          // 3️⃣ 토큰 검증 (만료 여부 포함)
          if (jwtTokenProvider.validateToken(token)) {

            // 4️⃣ 토큰에서 사용자 정보 추출
            String email = jwtTokenProvider.getEmailFromToken(token);
            Long userId = jwtTokenProvider.getUserIdFromToken(token);

            log.debug("JWT 토큰 검증 성공 - 이메일: {}, userId: {}", email, userId);

            // 5️⃣ DB에서 사용자 조회 (선택사항 - 성능을 위해 생략 가능)
            User user = userRepository.findById(userId)
                .orElse(null);

            if (user != null && user.getIsActive()) {
              // 6️⃣ Spring Security 인증 객체 생성
              // 권한 정보 생성 (예: ROLE_USER)
              List<SimpleGrantedAuthority> authorities = List.of(
                  new SimpleGrantedAuthority(user.getRole().name())
              );

              // 인증 토큰 생성 (principal: 사용자 정보, credentials: 비밀번호(null), authorities: 권한)
              UsernamePasswordAuthenticationToken authentication =
                  new UsernamePasswordAuthenticationToken(user, null, authorities);

              // 요청 정보 추가 (IP 주소 등)
              authentication.setDetails(
                  new WebAuthenticationDetailsSource().buildDetails(request)
              );

              // 7️⃣ SecurityContext에 인증 정보 설정
              // 이제 컨트롤러에서 @AuthenticationPrincipal로 사용자 정보에 접근 가능
              SecurityContextHolder.getContext().setAuthentication(authentication);

              log.debug("SecurityContext에 인증 정보 설정 완료: {}", email);
            }
          }

        } catch (ExpiredJwtException e) {
          // ⚠️ 토큰이 만료된 경우 - 클라이언트에게 /refresh 호출하도록 안내
          log.warn("JWT 토큰이 만료되었습니다 - 사용자: {}, 경로: {}", e.getClaims().getSubject(), request.getRequestURI());
          sendErrorResponse(response, JwtErrorResponse.tokenExpired(request.getRequestURI()), HttpStatus.UNAUTHORIZED);
          return; // 필터 체인 중단 (인증 실패로 더 이상 진행하지 않음)

        } catch (JwtException e) {
          // ❌ 유효하지 않은 토큰 (서명 오류, 형식 오류 등) - 클라이언트에게 재로그인 요구
          log.error("유효하지 않은 JWT 토큰: {}, 경로: {}", e.getMessage(), request.getRequestURI());
          sendErrorResponse(response, JwtErrorResponse.invalidToken(request.getRequestURI()), HttpStatus.UNAUTHORIZED);
          return; // 필터 체인 중단

        } catch (IllegalArgumentException e) {
          // ❌ 잘못된 형식의 토큰
          log.error("잘못된 형식의 JWT 토큰: {}, 경로: {}", e.getMessage(), request.getRequestURI());
          sendErrorResponse(response, JwtErrorResponse.invalidToken(request.getRequestURI()), HttpStatus.UNAUTHORIZED);
          return; // 필터 체인 중단
        }
      }

    } catch (Exception e) {
      // 예상치 못한 오류가 발생한 경우
      log.error("JWT 인증 처리 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
      // 예외가 발생해도 필터 체인은 계속 진행 (인증 실패로 처리됨)
    }

    // 8️⃣ 다음 필터로 요청 전달
    // 토큰이 없거나, 유효한 경우, 또는 예상치 못한 오류가 발생한 경우 여기까지 도달
    filterChain.doFilter(request, response);
  }

  /**
   * HTTP 요청의 Authorization 헤더에서 JWT 토큰 추출
   *
   * @param request HTTP 요청
   * @return JWT 토큰 (없으면 null)
   */
  private String extractTokenFromRequest(HttpServletRequest request) {
    // Authorization 헤더 값 가져오기
    String bearerToken = request.getHeader("Authorization");

    // "Bearer {token}" 형식인지 확인하고 토큰만 추출
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7); // "Bearer " 이후의 토큰 문자열 반환
    }

    return null;
  }

  /**
   * JWT 인증 실패 시 JSON 형식의 에러 응답 전송
   * 클라이언트가 토큰 만료와 유효하지 않은 토큰을 구분할 수 있도록 상세한 정보 제공
   *
   * @param response HTTP 응답 객체
   * @param errorResponse 에러 응답 DTO (errorCode, message, action 포함)
   * @param status HTTP 상태 코드 (401 Unauthorized)
   * @throws IOException JSON 직렬화 또는 응답 작성 중 오류 발생 시
   */
  private void sendErrorResponse(
      HttpServletResponse response,
      JwtErrorResponse errorResponse,
      HttpStatus status
  ) throws IOException {
    // 응답 설정
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    // JSON 직렬화 및 응답 작성 (Spring이 관리하는 ObjectMapper 사용)
    String jsonResponse = objectMapper.writeValueAsString(errorResponse);
    response.getWriter().write(jsonResponse);
  }
}
