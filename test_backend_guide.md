# 백엔드 API 테스트 가이드

이 문서는 myauth 백엔드 API의 주요 엔드포인트를 테스트하기 위한 curl 명령어 모음입니다.

## 목차
1. [헬스체크](#1-헬스체크)
2. [회원가입 (Signup)](#2-회원가입-signup)
3. [로그인 (Login)](#3-로그인-login)
4. [Access Token 갱신 (Refresh)](#4-access-token-갱신-refresh)
5. [로그아웃 (Logout)](#5-로그아웃-logout)
6. [카카오 OAuth 로그인](#6-카카오-oauth-로그인)
7. [인증이 필요한 엔드포인트 테스트](#7-인증이-필요한-엔드포인트-테스트)

---

## 환경 설정

### 개발 환경
```bash
# 개발 서버 URL
export API_URL="http://localhost:9080"
```

### 프로덕션 환경
```bash
# 프로덕션 서버 URL
export API_URL="http://15.165.81.224:8080"
```

---

## 1. 헬스체크

서버가 정상적으로 실행 중인지 확인합니다.

```bash
curl -X GET "${API_URL}/health" \
  -H "Content-Type: application/json"
```

**예상 응답:**
```json
{
  "success": true,
  "message": "Auth Service is running",
  "data": null
}
```

---

## 2. 회원가입 (Signup)

새로운 사용자를 등록합니다.

### 기본 회원가입

```bash
curl -X POST "${API_URL}/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123!",
    "name": "테스트유저"
  }'
```

### 다양한 테스트 케이스

```bash
# 테스트 사용자 1
curl -X POST "${API_URL}/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test1@example.com",
    "password": "Test1234!",
    "name": "김철수"
  }'

# 테스트 사용자 2
curl -X POST "${API_URL}/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test2@example.com",
    "password": "Test5678!",
    "name": "이영희"
  }'
```

**예상 응답 (성공):**
```json
{
  "success": true,
  "message": "회원가입이 완료되었습니다.",
  "data": null
}
```

**예상 응답 (실패 - 중복 이메일):**
```json
{
  "success": false,
  "message": "이미 사용 중인 이메일입니다",
  "data": null
}
```

---

## 3. 로그인 (Login)

이메일과 비밀번호로 로그인하여 JWT 토큰을 받습니다.

### 기본 로그인

```bash
curl -X POST "${API_URL}/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123!"
  }'
```

### 쿠키 포함 로그인 (웹 클라이언트 시뮬레이션)

웹 브라우저에서 요청하는 것처럼 User-Agent를 설정하면 Refresh Token이 쿠키로 전송됩니다.

```bash
curl -X POST "${API_URL}/login" \
  -H "Content-Type: application/json" \
  -H "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36" \
  -c cookies.txt \
  -d '{
    "email": "test@example.com",
    "password": "password123!"
  }'
```

### 모바일 클라이언트 시뮬레이션

```bash
curl -X POST "${API_URL}/login" \
  -H "Content-Type: application/json" \
  -H "User-Agent: MyApp/1.0 (iPhone; iOS 16.0)" \
  -d '{
    "email": "test@example.com",
    "password": "password123!"
  }'
```

**예상 응답 (웹 클라이언트 - Refresh Token이 쿠키로 전송됨):**
```json
{
  "success": true,
  "message": "로그인 성공",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": null,
    "tokenType": "Bearer",
    "expiresIn": 3600000,
    "userId": 1,
    "email": "test@example.com",
    "name": "테스트유저"
  }
}
```

**예상 응답 (모바일 클라이언트 - Refresh Token이 응답 바디에 포함됨):**
```json
{
  "success": true,
  "message": "로그인 성공",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600000,
    "userId": 1,
    "email": "test@example.com",
    "name": "테스트유저"
  }
}
```

**토큰 저장 (환경 변수):**
```bash
# 로그인 후 Access Token 저장
export ACCESS_TOKEN="eyJhbGciOiJIUzUxMiJ9..."
export REFRESH_TOKEN="eyJhbGciOiJIUzUxMiJ9..."
```

---

## 4. Access Token 갱신 (Refresh)

Refresh Token을 사용하여 새로운 Access Token을 발급받습니다.

### 웹 클라이언트 (쿠키 사용)

로그인 시 저장된 쿠키를 사용합니다.

```bash
curl -X POST "${API_URL}/refresh" \
  -H "Content-Type: application/json" \
  -H "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36" \
  -b cookies.txt
```

### 모바일 클라이언트 (요청 바디 사용)

```bash
curl -X POST "${API_URL}/refresh" \
  -H "Content-Type: application/json" \
  -H "User-Agent: MyApp/1.0 (iPhone; iOS 16.0)" \
  -d '{
    "refreshToken": "'"${REFRESH_TOKEN}"'"
  }'
```

**예상 응답:**
```json
{
  "success": true,
  "message": "Access Token이 갱신되었습니다",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600000
  }
}
```

---

## 5. 로그아웃 (Logout)

현재 사용자를 로그아웃하고 Refresh Token을 무효화합니다.

### 웹 클라이언트 (쿠키 사용)

```bash
curl -X POST "${API_URL}/logout" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36" \
  -b cookies.txt \
  -c cookies.txt
```

### 모바일 클라이언트 (요청 바디 사용)

```bash
curl -X POST "${API_URL}/logout" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -H "User-Agent: MyApp/1.0 (iPhone; iOS 16.0)" \
  -d '{
    "refreshToken": "'"${REFRESH_TOKEN}"'"
  }'
```

**예상 응답:**
```json
{
  "success": true,
  "message": "로그아웃되었습니다",
  "data": null
}
```

---

## 6. 카카오 OAuth 로그인

카카오 소셜 로그인 프로세스를 테스트합니다.

### 6.1. 카카오 로그인 페이지로 리다이렉트

브라우저에서 다음 URL을 열면 카카오 로그인 페이지로 이동합니다:

```bash
# 개발 환경
open "http://localhost:9080/auth/kakao/login"

# 프로덕션 환경
open "http://15.165.81.224:8080/auth/kakao/login"
```

또는 curl로 리다이렉트 URL을 확인:

```bash
curl -X GET "${API_URL}/auth/kakao/login" \
  -L \
  -v
```

### 6.2. 카카오 콜백 시뮬레이션

**주의:** 실제 카카오 로그인은 브라우저에서 진행해야 합니다.
curl로는 인증 코드를 받을 수 없으므로, 브라우저에서 로그인 후 리다이렉트된 URL을 확인하세요.

카카오 로그인 프로세스:
1. `/auth/kakao/login` → 카카오 로그인 페이지로 리다이렉트
2. 사용자가 카카오 로그인 수행
3. 카카오가 `/auth/kakao/callback?code=...`으로 콜백
4. 백엔드가 JWT 토큰 발급 후 프론트엔드로 리다이렉트

**콜백 URL 예시:**
```
http://localhost:9080/auth/kakao/callback?code=카카오에서_발급한_인증코드
```

---

## 7. 인증이 필요한 엔드포인트 테스트

Access Token을 사용하여 보호된 엔드포인트에 접근합니다.

### 사용자 정보 조회 (예시)

**주의:** 이 엔드포인트는 실제 구현된 것이 아니라 예시입니다.
실제 프로젝트에 구현된 보호된 엔드포인트로 교체하세요.

```bash
# Authorization 헤더에 Bearer Token 포함
curl -X GET "${API_URL}/api/users/me" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json"
```

### Token 없이 접근 (401 Unauthorized 테스트)

```bash
curl -X GET "${API_URL}/api/users/me" \
  -H "Content-Type: application/json"
```

**예상 응답:**
```json
{
  "error": "Unauthorized",
  "message": "인증이 필요합니다."
}
```

### 만료된 Token으로 접근 (401 Unauthorized 테스트)

```bash
curl -X GET "${API_URL}/api/users/me" \
  -H "Authorization: Bearer expired_token_here" \
  -H "Content-Type: application/json"
```

---

## 전체 플로우 테스트 시나리오

### 시나리오 1: 일반 회원가입 → 로그인 → API 호출 → 로그아웃

```bash
# 1. 회원가입
curl -X POST "${API_URL}/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "flow_test@example.com",
    "password": "FlowTest123!",
    "name": "플로우테스터"
  }'

# 2. 로그인 (모바일 클라이언트)
RESPONSE=$(curl -X POST "${API_URL}/login" \
  -H "Content-Type: application/json" \
  -H "User-Agent: TestClient/1.0" \
  -d '{
    "email": "flow_test@example.com",
    "password": "FlowTest123!"
  }')

echo $RESPONSE

# 3. Access Token 추출 (jq 필요)
# export ACCESS_TOKEN=$(echo $RESPONSE | jq -r '.data.accessToken')
# export REFRESH_TOKEN=$(echo $RESPONSE | jq -r '.data.refreshToken')

# 4. 보호된 API 호출
# curl -X GET "${API_URL}/api/users/me" \
#   -H "Authorization: Bearer ${ACCESS_TOKEN}"

# 5. 로그아웃
# curl -X POST "${API_URL}/logout" \
#   -H "Authorization: Bearer ${ACCESS_TOKEN}" \
#   -H "Content-Type: application/json" \
#   -d '{
#     "refreshToken": "'"${REFRESH_TOKEN}"'"
#   }'
```

### 시나리오 2: Token 갱신 플로우

```bash
# 1. 로그인
RESPONSE=$(curl -X POST "${API_URL}/login" \
  -H "Content-Type: application/json" \
  -H "User-Agent: TestClient/1.0" \
  -d '{
    "email": "test@example.com",
    "password": "password123!"
  }')

# 2. Token 추출 (jq 사용)
# export ACCESS_TOKEN=$(echo $RESPONSE | jq -r '.data.accessToken')
# export REFRESH_TOKEN=$(echo $RESPONSE | jq -r '.data.refreshToken')

# 3. Access Token이 만료되었다고 가정하고, Refresh Token으로 갱신
curl -X POST "${API_URL}/refresh" \
  -H "Content-Type: application/json" \
  -H "User-Agent: TestClient/1.0" \
  -d '{
    "refreshToken": "'"${REFRESH_TOKEN}"'"
  }'

# 4. 새로운 Access Token으로 API 호출
# curl -X GET "${API_URL}/api/users/me" \
#   -H "Authorization: Bearer ${NEW_ACCESS_TOKEN}"
```

---

## 에러 케이스 테스트

### 1. 잘못된 이메일 형식

```bash
curl -X POST "${API_URL}/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "invalid-email",
    "password": "password123!",
    "name": "테스트"
  }'
```

### 2. 짧은 비밀번호

```bash
curl -X POST "${API_URL}/signup" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test3@example.com",
    "password": "123",
    "name": "테스트"
  }'
```

### 3. 잘못된 로그인 정보

```bash
curl -X POST "${API_URL}/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "wrong_password"
  }'
```

### 4. 존재하지 않는 사용자

```bash
curl -X POST "${API_URL}/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "nonexistent@example.com",
    "password": "password123!"
  }'
```

### 5. 잘못된 Refresh Token

```bash
curl -X POST "${API_URL}/refresh" \
  -H "Content-Type: application/json" \
  -H "User-Agent: TestClient/1.0" \
  -d '{
    "refreshToken": "invalid_token_here"
  }'
```

---

## 유용한 팁

### 1. JSON 응답 보기 좋게 출력 (jq 사용)

```bash
curl -X GET "${API_URL}/health" | jq '.'
```

### 2. HTTP 상태 코드 확인

```bash
curl -X POST "${API_URL}/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123!"}' \
  -w "\nHTTP Status: %{http_code}\n"
```

### 3. 응답 헤더 포함

```bash
curl -X POST "${API_URL}/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123!"}' \
  -i
```

### 4. 쿠키 저장 및 재사용

```bash
# 쿠키 저장
curl -X POST "${API_URL}/login" \
  -H "Content-Type: application/json" \
  -H "User-Agent: Mozilla/5.0" \
  -d '{"email":"test@example.com","password":"password123!"}' \
  -c cookies.txt

# 저장된 쿠키 사용
curl -X POST "${API_URL}/refresh" \
  -H "User-Agent: Mozilla/5.0" \
  -b cookies.txt
```

### 5. 디버깅 (상세 로그)

```bash
curl -X POST "${API_URL}/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123!"}' \
  -v
```

---

## 데이터베이스 초기화 (개발용)

테스트 후 데이터베이스를 초기화하려면:

```bash
# MySQL 접속 (로컬 개발 환경)
mysql -u root -p1234 mannal

# 사용자 테이블 초기화
TRUNCATE TABLE users;
TRUNCATE TABLE refresh_tokens;
```

---

## 문제 해결

### CORS 에러 발생 시

개발 환경에서는 `localhost:5173` (프론트엔드)과 `localhost:9080` (백엔드)가 CORS 설정에 포함되어 있어야 합니다.

### 쿠키가 전송되지 않을 때

1. `User-Agent` 헤더를 웹 브라우저로 설정했는지 확인
2. `-c cookies.txt` (쿠키 저장)와 `-b cookies.txt` (쿠키 사용) 옵션을 올바르게 사용했는지 확인

### JWT Token 디코딩

JWT 토큰의 내용을 확인하려면 [jwt.io](https://jwt.io)에서 디코딩할 수 있습니다.

---

## 참고 문서

- [Spring Security 공식 문서](https://spring.io/projects/spring-security)
- [JWT 공식 사이트](https://jwt.io)
- [카카오 로그인 개발 가이드](https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api)
- [curl 공식 문서](https://curl.se/docs/)

---

**작성일:** 2025-12-28
**프로젝트:** myauth (Spring Boot JWT Authentication)
