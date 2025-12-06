# Frontend API Guide

> 백엔드 인증 서비스 API 문서 - 프론트엔드 개발자용

## 📋 목차

1. [기본 정보](#기본-정보)
2. [인증 방식](#인증-방식)
3. [API 엔드포인트](#api-엔드포인트)
4. [프론트엔드 구현 가이드](#프론트엔드-구현-가이드)
5. [에러 처리](#에러-처리)

---

## 기본 정보

### Base URL
```
http://localhost:9080
```

### Content-Type
```
application/json
```

### 응답 형식
모든 API는 다음 형식으로 응답합니다:

```json
{
  "success": true,
  "message": "응답 메시지",
  "data": { ... }
}
```

---

## 인증 방식

### JWT (JSON Web Token)

이 API는 JWT 기반 인증을 사용합니다.

#### Access Token
- 유효기간: **1시간**
- 용도: API 요청 시 인증
- 저장 위치: 메모리 (변수) 또는 SessionStorage
- 전송 방법: `Authorization: Bearer {token}` 헤더

#### Refresh Token
- 유효기간: **7일**
- 용도: Access Token 갱신
- 저장 위치:
  - 웹: HTTP-only 쿠키 (자동 관리)
  - 모바일: SecureStorage
- 전송 방법:
  - 웹: 자동 (쿠키)
  - 모바일: 요청 바디

---

## API 엔드포인트

### 1. 헬스 체크

서버 상태 확인

```http
GET /health
```

**응답 200 OK**
```json
{
  "success": true,
  "message": "Auth Service is running"
}
```

---

### 2. 회원가입

새 사용자 등록

```http
POST /signup
Content-Type: application/json
```

**요청 바디**
```json
{
  "email": "user@example.com",
  "password": "password123!",
  "name": "홍길동"
}
```

**유효성 검증**
- `email`: 이메일 형식, 필수
- `password`: 최소 8자, 필수
- `name`: 최소 2자, 필수

**응답 201 Created**
```json
{
  "success": true,
  "message": "회원가입이 완료되었습니다."
}
```

**에러 응답 400 Bad Request**
```json
{
  "success": false,
  "message": "이미 존재하는 이메일입니다."
}
```

---

### 3. 로그인

#### 옵션 1: 간단한 로그인 (권장)

```http
POST /login
Content-Type: application/json
```

**요청 바디**
```json
{
  "email": "user@example.com",
  "password": "password123!"
}
```

**응답 200 OK**
```json
{
  "success": true,
  "message": "로그인 성공",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "user": {
      "id": 1,
      "email": "user@example.com",
      "name": "홍길동",
      "role": "ROLE_USER"
    }
  }
}
```

#### 옵션 2: 하이브리드 로그인 (웹/모바일 구분)

```http
POST /loginEx
Content-Type: application/json
```

**요청/응답**: `/login`과 동일

**차이점**:
- **웹 브라우저**: Refresh Token이 HTTP-only 쿠키로 자동 저장됨 (응답 바디에는 없음)
- **모바일 앱**: Refresh Token이 응답 바디에 포함됨

---

### 4. Access Token 갱신

Access Token 만료 시 새 토큰 발급

```http
POST /refresh
Content-Type: application/json
```

**요청 바디 (모바일만)**
```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

> **웹 브라우저**: 요청 바디 없음 (쿠키에서 자동으로 읽음)

**응답 200 OK**
```json
{
  "success": true,
  "message": "Access Token이 갱신되었습니다",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "user": {
      "id": 1,
      "email": "user@example.com"
    }
  }
}
```

**에러 응답 401 Unauthorized**
```json
{
  "success": false,
  "message": "유효하지 않거나 만료된 Refresh Token입니다."
}
```

---

### 5. 로그아웃

현재 세션 종료

```http
POST /logout
Authorization: Bearer {accessToken}
```

**응답 200 OK**
```json
{
  "success": true,
  "message": "로그아웃 성공"
}
```

> **참고**: 카카오 로그인 사용자도 동일한 엔드포인트 사용

---

### 6. 카카오 로그인

#### 6-1. 카카오 로그인 시작

사용자를 카카오 로그인 페이지로 리다이렉트

```http
GET /auth/kakao/login
```

**응답**: 302 Redirect
```
Location: https://kauth.kakao.com/oauth/authorize?client_id=...
```

**프론트엔드 구현 예시**
```javascript
// 카카오 로그인 버튼 클릭 시
window.location.href = 'http://localhost:9080/auth/kakao/login';
```

#### 6-2. 카카오 로그인 콜백 (하이브리드 방식)

카카오 인증 후 자동 호출됨 (프론트엔드에서 직접 호출 안 함)

```http
GET /auth/kakao/callback?code={AUTHORIZATION_CODE}
```

**클라이언트 타입별 차이점**:
- **웹 브라우저**: Refresh Token이 HTTP-only 쿠키로 자동 저장됨 (응답 바디에는 없음)
- **모바일 앱**: Refresh Token이 응답 바디에 포함됨

**응답 200 OK (웹 브라우저)**
```json
{
  "success": true,
  "message": "카카오 로그인 성공",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": null,  // 웹: 쿠키로 전송되므로 null
    "user": {
      "id": 42,
      "email": "kakao@example.com",
      "name": "홍길동",
      "role": "ROLE_USER"
    }
  }
}
```

> **웹 브라우저**: `Set-Cookie: refreshToken=eyJhbGci...; HttpOnly; Path=/; Max-Age=604800`

**응답 200 OK (모바일 앱)**
```json
{
  "success": true,
  "message": "카카오 로그인 성공",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",  // 모바일: JSON에 포함
    "user": {
      "id": 42,
      "email": "kakao@example.com",
      "name": "홍길동",
      "role": "ROLE_USER"
    }
  }
}
```

---

### 7. 보호된 API 호출 예시

인증이 필요한 API 호출 시

```http
GET /api/protected-resource
Authorization: Bearer {accessToken}
```

**인증 실패 시 401 Unauthorized**
```json
{
  "error": "Unauthorized",
  "message": "인증이 필요합니다."
}
```

---

## 프론트엔드 구현 가이드

### 웹 (React / Vue / Angular)

#### 1. 로그인

```javascript
// 로그인 API 호출
async function login(email, password) {
  const response = await fetch('http://localhost:9080/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ email, password }),
  });

  const result = await response.json();

  if (result.success) {
    // Access Token 저장 (메모리 또는 SessionStorage)
    sessionStorage.setItem('accessToken', result.data.accessToken);

    // Refresh Token 저장 (선택)
    sessionStorage.setItem('refreshToken', result.data.refreshToken);

    // 사용자 정보 저장
    localStorage.setItem('user', JSON.stringify(result.data.user));

    return result.data;
  } else {
    throw new Error(result.message);
  }
}
```

#### 2. 인증이 필요한 API 호출

```javascript
async function fetchProtectedData() {
  const accessToken = sessionStorage.getItem('accessToken');

  const response = await fetch('http://localhost:9080/api/protected', {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${accessToken}`,
    },
  });

  if (response.status === 401) {
    // Access Token 만료 → 갱신 시도
    await refreshAccessToken();

    // 재시도
    return fetchProtectedData();
  }

  return response.json();
}
```

#### 3. Access Token 갱신

```javascript
async function refreshAccessToken() {
  const refreshToken = sessionStorage.getItem('refreshToken');

  const response = await fetch('http://localhost:9080/refresh', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ refreshToken }),
  });

  const result = await response.json();

  if (result.success) {
    // 새 Access Token 저장
    sessionStorage.setItem('accessToken', result.data.accessToken);
  } else {
    // Refresh Token도 만료 → 재로그인 필요
    sessionStorage.clear();
    window.location.href = '/login';
  }
}
```

#### 4. 로그아웃

```javascript
async function logout() {
  const accessToken = sessionStorage.getItem('accessToken');

  await fetch('http://localhost:9080/logout', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${accessToken}`,
    },
  });

  // 로컬 저장소 정리
  sessionStorage.clear();
  localStorage.removeItem('user');

  // 로그인 페이지로 이동
  window.location.href = '/login';
}
```

#### 5. 카카오 로그인

```javascript
function loginWithKakao() {
  // 카카오 로그인 페이지로 이동
  window.location.href = 'http://localhost:9080/auth/kakao/login';
}

// 카카오 콜백 페이지 (/auth/kakao/callback)에서:
// URL에서 토큰 정보를 추출하거나,
// 백엔드가 프론트엔드 콜백 URL로 리다이렉트하도록 설정
```

---

### 모바일 (React Native / Flutter)

#### 1. Axios 인터셉터 설정 (React Native 예시)

```javascript
import axios from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';

const api = axios.create({
  baseURL: 'http://localhost:9080',
  headers: {
    'Content-Type': 'application/json',
  },
});

// 요청 인터셉터: Access Token 자동 추가
api.interceptors.request.use(
  async (config) => {
    const accessToken = await AsyncStorage.getItem('accessToken');
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 응답 인터셉터: 401 에러 시 자동 토큰 갱신
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = await AsyncStorage.getItem('refreshToken');
        const response = await axios.post('http://localhost:9080/refresh', {
          refreshToken,
        });

        const newAccessToken = response.data.data.accessToken;
        await AsyncStorage.setItem('accessToken', newAccessToken);

        // 원래 요청 재시도
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return axios(originalRequest);
      } catch (refreshError) {
        // Refresh Token도 만료 → 로그아웃 처리
        await AsyncStorage.clear();
        // 로그인 화면으로 이동
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default api;
```

#### 2. 로그인 구현

```javascript
import api from './api';
import AsyncStorage from '@react-native-async-storage/async-storage';

async function login(email, password) {
  const response = await api.post('/login', { email, password });

  if (response.data.success) {
    const { accessToken, refreshToken, user } = response.data.data;

    // 토큰 저장
    await AsyncStorage.setItem('accessToken', accessToken);
    await AsyncStorage.setItem('refreshToken', refreshToken);
    await AsyncStorage.setItem('user', JSON.stringify(user));

    return user;
  }
}
```

---

## 에러 처리

### HTTP 상태 코드

| 코드 | 의미 | 예시 |
|------|------|------|
| 200 | 성공 | 로그인 성공, 데이터 조회 성공 |
| 201 | 생성 성공 | 회원가입 성공 |
| 400 | 잘못된 요청 | 유효성 검증 실패, 중복 이메일 |
| 401 | 인증 실패 | 토큰 만료, 잘못된 비밀번호 |
| 403 | 권한 없음 | 관리자 전용 API를 일반 사용자가 호출 |
| 500 | 서버 에러 | 서버 내부 오류 |

### 에러 응답 형식

```json
{
  "success": false,
  "message": "에러 메시지"
}
```

### 일반적인 에러 메시지

| 에러 메시지 | 원인 | 해결 방법 |
|-----------|------|----------|
| "이미 존재하는 이메일입니다." | 회원가입 시 중복 이메일 | 다른 이메일 사용 |
| "이메일 또는 비밀번호가 올바르지 않습니다." | 로그인 실패 | 이메일/비밀번호 확인 |
| "인증이 필요합니다." | Access Token 없음 또는 만료 | 토큰 갱신 또는 재로그인 |
| "유효하지 않거나 만료된 Refresh Token입니다." | Refresh Token 만료 | 재로그인 필요 |

---

## 토큰 관리 모범 사례

### Access Token 저장

| 플랫폼 | 권장 저장소 | 이유 |
|--------|------------|------|
| 웹 | SessionStorage 또는 메모리 | XSS 공격 방어 |
| 모바일 | 메모리 (변수) | 빠른 접근 |

### Refresh Token 저장

| 플랫폼 | 권장 저장소 | 이유 |
|--------|------------|------|
| 웹 | HTTP-only 쿠키 | XSS 공격 완전 방어 |
| 모바일 | SecureStorage / Keychain | 암호화 저장 |

### 보안 주의사항

1. **절대 LocalStorage에 토큰 저장 금지** (XSS 공격 위험)
2. **HTTPS 사용 필수** (프로덕션 환경)
3. **토큰을 URL 파라미터로 전송 금지**
4. **토큰 만료 시 자동 갱신 구현**

---

## 카카오 로그인 플로우

```
1. 사용자가 "카카오 로그인" 버튼 클릭
   ↓
2. 프론트엔드: GET /auth/kakao/login 호출
   ↓
3. 백엔드: 카카오 로그인 페이지로 리다이렉트 (302)
   ↓
4. 사용자가 카카오에서 로그인 및 동의
   ↓
5. 카카오: 백엔드 콜백 URL로 리다이렉트 (Authorization Code 포함)
   ↓
6. 백엔드: 카카오 API로 토큰 요청 및 사용자 정보 조회
   ↓
7. 백엔드: JWT 발급 및 JSON 응답
   ↓
8. 프론트엔드: 토큰 저장 및 로그인 처리
```

### 카카오 로그인 구현 시 주의사항

1. **이메일 필수 동의 설정**
   - 카카오 개발자 콘솔에서 이메일을 "필수 동의" 항목으로 설정
   - 이메일이 없으면 회원가입 실패

2. **콜백 처리**
   - 백엔드 콜백 URL: `http://localhost:9080/auth/kakao/callback`
   - 프론트엔드는 이 URL로 리다이렉트된 후 토큰을 받음
   - 백엔드에서 프론트엔드 URL로 최종 리다이렉트 설정 가능

3. **로그아웃**
   - 카카오 로그인 사용자도 `/logout` 사용
   - 카카오 서버 세션은 유지됨 (재로그인 시 자동 로그인)

---

## 개발 환경 설정

### CORS 설정

백엔드에서 프론트엔드 도메인을 허용해야 합니다.

**개발 환경**: `http://localhost:3000` (React 기본 포트)

필요 시 백엔드 개발자에게 CORS 설정 요청하세요.

### API 테스트 도구

- **Postman**: API 엔드포인트 테스트
- **cURL**: 커맨드라인 테스트
- **브라우저 개발자 도구**: 네트워크 탭에서 요청/응답 확인

---

## 빠른 시작 가이드

### 1. 회원가입 및 로그인

```javascript
// 1. 회원가입
await fetch('http://localhost:9080/signup', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    email: 'test@example.com',
    password: 'password123!',
    name: '홍길동'
  })
});

// 2. 로그인
const loginRes = await fetch('http://localhost:9080/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    email: 'test@example.com',
    password: 'password123!'
  })
});

const { data } = await loginRes.json();
const accessToken = data.accessToken; // 이 토큰을 저장하고 사용
```

### 2. 인증이 필요한 API 호출

```javascript
const response = await fetch('http://localhost:9080/api/protected', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
});
```

### 3. 로그아웃

```javascript
await fetch('http://localhost:9080/logout', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
});

// 토큰 삭제
sessionStorage.clear();
```

---

## FAQ

### Q1. Access Token이 만료되면 어떻게 하나요?

A: `/refresh` 엔드포인트를 호출하여 새 Access Token을 발급받으세요. Refresh Token도 만료되었다면 재로그인이 필요합니다.

### Q2. 웹과 모바일에서 Refresh Token 관리가 다른 이유는?

A: 웹에서는 HTTP-only 쿠키를 사용하여 JavaScript에서 접근할 수 없게 하여 XSS 공격을 방어합니다. 모바일은 쿠키를 사용할 수 없으므로 SecureStorage에 암호화하여 저장합니다.

### Q3. 카카오 로그인 후 토큰은 어디서 받나요?

A: 백엔드의 `/auth/kakao/callback` 엔드포인트에서 JSON 형태로 토큰을 반환합니다. 프론트엔드는 이 응답을 처리하여 토큰을 저장하면 됩니다.

### Q4. 로그인 상태를 어떻게 유지하나요?

A: Access Token을 SessionStorage나 메모리에 저장하고, Refresh Token으로 주기적으로 갱신합니다. 페이지 새로고침 시에도 로그인 상태를 유지하려면 Refresh Token을 사용하세요.

### Q5. 프로덕션 배포 시 주의사항은?

A:
- HTTPS 사용 필수
- 쿠키 Secure 플래그 활성화
- Base URL을 실제 도메인으로 변경
- CORS 설정 확인
- 토큰 만료 시간 조정 검토

---

**문서 작성일**: 2025-12-06
**백엔드 서버**: Spring Boot 4.0.0
**인증 방식**: JWT (HS512)
