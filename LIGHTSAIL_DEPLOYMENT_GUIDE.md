# AWS Lightsail 배포 가이드

> **이 가이드의 목표:** Spring Boot 백엔드 애플리케이션을 AWS Lightsail 서버에 Docker 컨테이너로 자동 배포하는 것입니다.
>
> **배포 방식:** GitHub에 코드를 푸시하면 자동으로 빌드되고 서버에 배포됩니다 (GitHub Actions 사용).
>
> **대상 독자:** Docker와 GitHub Actions를 처음 사용하는 초보자도 이해할 수 있도록 작성되었습니다.

---

## 목차

1. [배포 아키텍처 이해하기](#1-배포-아키텍처-이해하기)
2. [사전 준비사항](#2-사전-준비사항)
3. [Lightsail 서버 초기 설정](#3-lightsail-서버-초기-설정)
4. [GitHub Secrets 설정](#4-github-secrets-설정)
5. [서버에 필요한 파일 준비](#5-서버에-필요한-파일-준비)
6. [배포 실행하기](#6-배포-실행하기)
7. [배포 확인하기](#7-배포-확인하기)
8. [문제 해결 가이드](#8-문제-해결-가이드)

---

## 1. 배포 아키텍처 이해하기

### 전체 흐름 다이어그램

```
┌─────────────────┐
│  개발자 PC      │
│  (로컬 환경)    │
└────────┬────────┘
         │
         │ git push origin main
         │ (코드를 GitHub에 업로드)
         ↓
┌─────────────────┐
│ GitHub Repository│
│ (코드 저장소)    │
└────────┬────────┘
         │
         │ 자동 트리거
         ↓
┌─────────────────────────────────────────┐
│      GitHub Actions (CI/CD)             │
│ ┌─────────────────────────────────────┐ │
│ │ 1단계: 코드 체크아웃               │ │
│ │ 2단계: Java 17 설정                │ │
│ │ 3단계: Gradle로 JAR 빌드           │ │
│ │ 4단계: Docker 이미지 생성          │ │
│ │ 5단계: GHCR에 이미지 업로드        │ │
│ │ 6단계: SSH로 Lightsail 접속        │ │
│ │ 7단계: 컨테이너 재시작             │ │
│ └─────────────────────────────────────┘ │
└────────────┬────────────────────────────┘
             │
             │ SSH 접속 및 배포
             ↓
    ┌──────────────────────────┐
    │ AWS Lightsail 서버       │
    │ IP: 13.124.241.22        │
    │ ┌──────────────────────┐ │
    │ │ MySQL 컨테이너       │ │
    │ │ - 데이터베이스       │ │
    │ │ - 포트: 3306         │ │
    │ └──────────────────────┘ │
    │ ┌──────────────────────┐ │
    │ │ Spring Boot 컨테이너 │ │
    │ │ - 백엔드 API         │ │
    │ │ - 포트: 9080         │ │
    │ └──────────────────────┘ │
    └──────────────────────────┘
```

### 각 구성 요소 설명

#### 1. 개발자 PC (로컬 환경)
- **역할:** 코드를 작성하고 GitHub에 푸시합니다.
- **필요한 작업:** `git push origin main` 명령만 실행하면 됩니다.
- **추가 작업 불필요:** 직접 빌드하거나 서버에 업로드할 필요 없습니다.

#### 2. GitHub Repository (코드 저장소)
- **역할:** 소스 코드를 저장하고 버전 관리를 합니다.
- **트리거:** main 브랜치에 코드가 푸시되면 자동으로 GitHub Actions를 실행합니다.

#### 3. GitHub Actions (CI/CD 자동화)
- **역할:** 빌드, 테스트, 배포를 자동으로 수행합니다.
- **실행 환경:** GitHub가 제공하는 클라우드 서버(Ubuntu)에서 실행됩니다.
- **비용:** 공개 저장소는 무료, 비공개 저장소는 월 2,000분까지 무료입니다.

**세부 단계:**
1. **코드 체크아웃:** GitHub 저장소에서 최신 코드를 가져옵니다.
2. **Java 17 설정:** Spring Boot를 빌드하기 위한 Java 환경을 설정합니다.
3. **Gradle 빌드:** `./gradlew bootJar` 명령으로 실행 가능한 JAR 파일을 생성합니다.
4. **Docker 이미지 생성:** JAR 파일을 포함한 Docker 이미지를 만듭니다.
5. **GHCR 업로드:** 생성한 이미지를 GitHub Container Registry에 저장합니다.
6. **SSH 접속:** Lightsail 서버에 안전하게 접속합니다.
7. **배포 실행:** 서버에서 최신 이미지를 다운로드하고 컨테이너를 재시작합니다.

#### 4. GHCR (GitHub Container Registry)
- **역할:** Docker 이미지를 저장하는 저장소입니다.
- **주소:** ghcr.io
- **비용:** 공개 이미지는 무료, 비공개 이미지는 500MB까지 무료입니다.
- **장점:** GitHub와 완벽하게 통합되어 별도 설정이 거의 필요 없습니다.

#### 5. AWS Lightsail (프로덕션 서버)
- **역할:** 실제 애플리케이션이 실행되는 클라우드 서버입니다.
- **IP 주소:** 13.124.241.22 (고정 IP)
- **OS:** Amazon Linux 2023
- **Docker 컨테이너:**
  - **MySQL 컨테이너:** 데이터베이스 (포트 3306)
  - **Spring Boot 컨테이너:** 백엔드 API (포트 9080)

### 배포 방식의 장점

1. **완전 자동화**
   - 코드를 푸시하면 모든 과정이 자동으로 진행됩니다.
   - 수동 작업이 없어 실수가 줄어듭니다.

2. **무중단 배포**
   - 새 버전이 배포되는 동안에도 서비스가 중단되지 않습니다.
   - Docker Compose가 자동으로 컨테이너를 교체합니다.

3. **쉬운 롤백**
   - 문제가 생기면 이전 Docker 이미지로 쉽게 되돌릴 수 있습니다.
   - GHCR에 모든 버전이 저장되어 있습니다.

4. **환경 일관성**
   - Docker 컨테이너를 사용하여 개발/프로덕션 환경이 동일합니다.
   - "내 컴퓨터에서는 됐는데..." 문제가 사라집니다.

---

## 2. 사전 준비사항

배포를 시작하기 전에 다음 항목들을 준비해야 합니다.

### ✅ 체크리스트

#### 필수 항목

- [ ] **AWS Lightsail 인스턴스** 생성 완료
- [ ] **SSH 키 파일** (`.pem` 파일) 다운로드 및 보관
- [ ] **GitHub 저장소** 생성 및 코드 업로드 완료
- [ ] **Lightsail IP 주소** 확인 (예: 13.124.241.22)

#### 권장 항목

- [ ] **도메인** (선택사항, 없으면 IP 주소 사용)
- [ ] **카카오 개발자 계정** (OAuth 로그인용)

### 1. AWS Lightsail 인스턴스

#### 필요한 사양
- **IP 주소:** 13.124.241.22 (고정 IP)
- **운영체제:** Amazon Linux 2023 또는 Ubuntu 22.04
- **최소 사양:**
  - RAM: 1GB 이상 (권장: 2GB)
  - vCPU: 1코어 이상
  - 디스크: 20GB 이상 (권장: 40GB)
- **월 비용:** 약 $3.50 ~ $5 (가장 저렴한 플랜)

#### 개방해야 할 포트
Lightsail 방화벽 설정에서 다음 포트를 개방해야 합니다:

| 포트 | 프로토콜 | 용도 | 필수 여부 |
|------|----------|------|-----------|
| 22 | TCP | SSH (서버 접속) | 필수 |
| 80 | TCP | HTTP (웹 서비스) | 필수 |
| 443 | TCP | HTTPS (SSL) | 권장 |
| 9080 | TCP | Spring Boot (개발용) | 선택 |

#### Lightsail 인스턴스 생성 방법
1. AWS Lightsail 콘솔 접속: https://lightsail.aws.amazon.com/
2. "인스턴스 생성" 클릭
3. 설정:
   - **위치:** 서울 (ap-northeast-2)
   - **플랫폼:** Linux/Unix
   - **운영체제:** Amazon Linux 2023
   - **플랜:** $3.50 (512MB RAM, 1 vCPU)
4. "인스턴스 생성" 완료
5. **고정 IP 연결** (무료)
6. **SSH 키 다운로드** (.pem 파일) - 꼭 안전한 곳에 보관!

### 2. GitHub 저장소

#### 필요한 설정
- **저장소 종류:** Public 또는 Private (무료)
- **필수 파일:**
  - `.github/workflows/deploy.yml` (GitHub Actions 워크플로우)
  - `Dockerfile` (Docker 이미지 빌드 설정)
  - `docker-compose.prod.yml` (프로덕션 컨테이너 설정)
  - `.env.example` (환경 변수 템플릿)

#### GitHub 저장소 준비
```bash
# 로컬에서 GitHub 저장소와 연결
git remote add origin https://github.com/your-username/myauth.git

# 코드 푸시
git add .
git commit -m "Initial commit"
git push -u origin main
```

### 3. SSH 키 파일

#### SSH 키란?
- Lightsail 서버에 안전하게 접속하기 위한 비밀 열쇠입니다.
- Lightsail 인스턴스 생성 시 자동으로 생성됩니다.
- 확장자: `.pem` (예: `LightsailDefaultKey-ap-northeast-2.pem`)

#### SSH 키 다운로드 및 보관
1. Lightsail 콘솔 → 계정 → SSH 키
2. 기본 키 다운로드 또는 새 키 생성
3. **중요:** 이 파일을 안전한 곳에 보관하세요!
4. **절대 금지:** GitHub에 업로드하거나 공개하지 마세요!

#### SSH 키 권한 설정 (Mac/Linux)
```bash
# SSH 키 파일 권한을 읽기 전용으로 변경 (필수)
chmod 400 ~/Downloads/LightsailDefaultKey-ap-northeast-2.pem

# 안전한 위치로 이동 (권장)
mkdir -p ~/.ssh
mv ~/Downloads/LightsailDefaultKey-ap-northeast-2.pem ~/.ssh/
```

#### SSH 접속 테스트
```bash
# Amazon Linux 2023
ssh -i ~/.ssh/LightsailDefaultKey-ap-northeast-2.pem ec2-user@13.124.241.22

# Ubuntu
ssh -i ~/.ssh/LightsailDefaultKey-ap-northeast-2.pem ubuntu@13.124.241.22

# 성공하면 서버 터미널이 열립니다
```

### 4. 카카오 개발자 계정 (OAuth용)

#### 카카오 애플리케이션 생성
1. https://developers.kakao.com/ 접속
2. 내 애플리케이션 → 애플리케이션 추가하기
3. REST API 키 복사 (KAKAO_CLIENT_ID)
4. 보안 탭 → Client Secret 활성화 및 복사 (KAKAO_CLIENT_SECRET)
5. 플랫폼 설정 → Web → Redirect URI 추가:
   ```
   http://13.124.241.22/auth/kakao/callback
   ```

---

## 3. Lightsail 서버 초기 설정

이 섹션에서는 새로 만든 Lightsail 서버에 Docker와 Docker Compose를 설치합니다.

### 단계 1: SSH로 서버 접속하기

#### 접속 명령어

**Amazon Linux 2023의 경우:**
```bash
ssh -i ~/.ssh/LightsailDefaultKey-ap-northeast-2.pem ec2-user@13.124.241.22
```

**Ubuntu의 경우:**
```bash
ssh -i ~/.ssh/LightsailDefaultKey-ap-northeast-2.pem ubuntu@13.124.241.22
```

#### 명령어 설명
- `ssh`: 원격 서버에 안전하게 접속하는 명령
- `-i ~/.ssh/LightsailDefaultKey-ap-northeast-2.pem`: 인증에 사용할 SSH 키 파일 지정
- `ec2-user` 또는 `ubuntu`: 서버 접속 사용자명
- `13.124.241.22`: 서버 IP 주소

#### 접속 성공 확인
접속에 성공하면 다음과 같은 화면이 나타납니다:
```
       __|  __|_  )
       _|  (     /   Amazon Linux 2023
      ___|\___|___|

Last login: Thu Dec 26 10:30:00 2024 from xxx.xxx.xxx.xxx
[ec2-user@ip-172-26-x-x ~]$
```

### 단계 2: 시스템 업데이트

서버의 모든 패키지를 최신 버전으로 업데이트합니다.

**Amazon Linux 2023:**
```bash
# 시스템 패키지 업데이트 (3-5분 소요)
sudo dnf update -y
```

**Ubuntu:**
```bash
# 패키지 목록 업데이트
sudo apt update

# 설치된 패키지 업그레이드
sudo apt upgrade -y
```

#### 명령어 설명
- `sudo`: 관리자 권한으로 실행
- `dnf` / `apt`: 패키지 관리자 (Amazon Linux는 dnf, Ubuntu는 apt)
- `update`: 패키지 목록 갱신
- `upgrade`: 실제 패키지 업그레이드
- `-y`: 모든 질문에 자동으로 "예" 응답

### 단계 3: Docker 설치

Docker는 컨테이너를 실행하는 핵심 프로그램입니다.

**Amazon Linux 2023:**
```bash
# 1. Docker 설치
sudo dnf install docker -y

# 2. Docker 서비스 시작
sudo systemctl start docker

# 3. 부팅 시 자동 시작 설정
sudo systemctl enable docker

# 4. 현재 사용자를 docker 그룹에 추가 (sudo 없이 docker 명령 사용)
sudo usermod -aG docker $USER

# 5. 설치 확인
docker --version
# 출력 예: Docker version 25.0.0, build xxx
```

**Ubuntu:**
```bash
# 1. Docker 설치
sudo apt install docker.io -y

# 2. Docker 서비스 시작
sudo systemctl start docker

# 3. 부팅 시 자동 시작 설정
sudo systemctl enable docker

# 4. 현재 사용자를 docker 그룹에 추가
sudo usermod -aG docker $USER

# 5. 설치 확인
docker --version
```

#### 중요: 재접속 필요
그룹 권한이 적용되려면 SSH를 종료하고 다시 접속해야 합니다:
```bash
# 현재 SSH 세션 종료
exit

# 다시 접속
ssh -i ~/.ssh/LightsailDefaultKey-ap-northeast-2.pem ec2-user@13.124.241.22
```

#### Docker 작동 테스트
```bash
# sudo 없이 docker 명령이 실행되는지 확인
docker ps
# 출력: CONTAINER ID   IMAGE   ...  (빈 목록이면 정상)
```

### 단계 4: Docker Compose 설치

Docker Compose는 여러 컨테이너를 한 번에 관리하는 도구입니다.

```bash
# 1. 최신 버전 다운로드 및 설치
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

# 2. 실행 권한 부여
sudo chmod +x /usr/local/bin/docker-compose

# 3. 설치 확인
docker-compose --version
# 출력 예: Docker Compose version v2.24.0
```

#### 명령어 설명
- `curl -L`: URL에서 파일 다운로드 (리다이렉션 따라가기)
- `$(uname -s)`: 현재 OS 이름 (Linux)
- `$(uname -m)`: 현재 CPU 아키텍처 (x86_64 또는 aarch64)
- `-o /usr/local/bin/docker-compose`: 다운로드한 파일을 저장할 경로
- `chmod +x`: 파일을 실행 가능하게 만들기

#### 설치 문제 해결
만약 `docker-compose` 명령이 작동하지 않으면:
```bash
# 심볼릭 링크 생성
sudo ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose

# 다시 확인
docker-compose --version
```

### ✅ 설치 완료 확인

모든 설치가 완료되었는지 확인합니다:

```bash
# Docker 버전 확인
docker --version

# Docker Compose 버전 확인
docker-compose --version

# Docker 서비스 상태 확인
sudo systemctl status docker

# 현재 사용자가 docker 그룹에 속해있는지 확인
groups
# 출력에 "docker"가 포함되어 있어야 함
```

---

## 4. GitHub Secrets 설정

GitHub Secrets는 비밀번호, SSH 키 같은 민감한 정보를 안전하게 저장하는 곳입니다.
이 정보들은 GitHub Actions 워크플로우에서 사용되지만, 코드에는 노출되지 않습니다.

### GitHub Secrets가 필요한 이유

배포 과정에서 다음 정보들이 필요합니다:
- Lightsail 서버 IP 주소
- SSH 접속 사용자명
- SSH 비밀 키

이 정보들을 코드에 직접 작성하면 **보안 위험**이 있으므로, GitHub Secrets에 안전하게 저장합니다.

### GitHub Secrets 페이지 접속 방법

1. GitHub에서 저장소 페이지 열기
2. 상단 메뉴에서 **Settings** 클릭
3. 왼쪽 사이드바에서 **Secrets and variables** → **Actions** 클릭
4. **New repository secret** 버튼 클릭

또는 URL로 직접 접속:
```
https://github.com/your-username/myauth/settings/secrets/actions
```

### 추가해야 할 Secrets (총 3개)

아래 3개의 Secret을 하나씩 추가합니다.

---

#### Secret 1: LIGHTSAIL_HOST

Lightsail 서버의 IP 주소를 저장합니다.

**설정 방법:**
1. **New repository secret** 클릭
2. **Name** 입력란에 정확히 입력:
   ```
   LIGHTSAIL_HOST
   ```
3. **Secret** 입력란에 IP 주소 입력:
   ```
   13.124.241.22
   ```
4. **Add secret** 클릭

**주의사항:**
- 대소문자 구분! 반드시 `LIGHTSAIL_HOST`로 입력 (소문자나 다른 철자 불가)
- IP 주소 앞뒤로 공백 없이 입력

---

#### Secret 2: LIGHTSAIL_USER

SSH 접속 시 사용할 사용자명을 저장합니다.

**설정 방법:**
1. **New repository secret** 클릭
2. **Name** 입력란:
   ```
   LIGHTSAIL_USER
   ```
3. **Secret** 입력란 (운영체제에 따라 다름):
   - **Amazon Linux 2023:**
     ```
     ec2-user
     ```
   - **Ubuntu:**
     ```
     ubuntu
     ```
4. **Add secret** 클릭

**어떤 사용자명을 입력해야 하나요?**
- Lightsail 인스턴스 생성 시 선택한 운영체제에 따라 결정됩니다
- Amazon Linux 2023을 선택했다면 → `ec2-user`
- Ubuntu를 선택했다면 → `ubuntu`

---

#### Secret 3: LIGHTSAIL_SSH_KEY (중요!)

SSH 접속에 사용할 비밀 키의 전체 내용을 저장합니다.

**설정 방법:**

**Step 1: SSH 키 파일 내용 확인**

터미널에서 SSH 키 파일 내용을 출력합니다:

```bash
# Mac/Linux
cat ~/.ssh/LightsailDefaultKey-ap-northeast-2.pem

# Windows (Git Bash)
cat ~/.ssh/LightsailDefaultKey-ap-northeast-2.pem

# Windows (PowerShell)
Get-Content ~\.ssh\LightsailDefaultKey-ap-northeast-2.pem
```

**Step 2: 출력 결과 전체 복사**

출력된 내용이 다음과 같은 형식이어야 합니다:

```
-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEAxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
... (여러 줄의 암호화된 텍스트)
xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
-----END RSA PRIVATE KEY-----
```

**중요:**
- `-----BEGIN` 부터 `-----END` 까지 **전체 내용**을 복사하세요!
- 앞뒤로 공백이나 추가 문자가 없어야 합니다
- 줄바꿈도 그대로 복사되어야 합니다

**Step 3: GitHub Secret에 추가**

1. **New repository secret** 클릭
2. **Name** 입력란:
   ```
   LIGHTSAIL_SSH_KEY
   ```
3. **Secret** 입력란에 **전체 키 내용 붙여넣기**
4. **Add secret** 클릭

**자주 발생하는 실수:**
- ❌ 키 파일의 일부만 복사
- ❌ `-----BEGIN` 또는 `-----END` 줄을 빠뜨림
- ❌ 복사할 때 앞뒤로 공백이 추가됨
- ✅ 전체 내용을 정확히 복사 (BEGIN부터 END까지)

---

### ✅ Secret 설정 완료 확인

3개의 Secret이 모두 추가되었는지 확인합니다.

GitHub → Settings → Secrets and variables → Actions 페이지에서 다음 3개가 보여야 합니다:

```
LIGHTSAIL_HOST
LIGHTSAIL_SSH_KEY
LIGHTSAIL_USER
```

### 자동 제공되는 Secret: GITHUB_TOKEN

추가로 `GITHUB_TOKEN`이라는 Secret이 필요하지만, 이것은 **GitHub이 자동으로 생성하고 제공**합니다.
따라서 별도로 추가할 필요가 없습니다!

**GITHUB_TOKEN의 용도:**
- GHCR(GitHub Container Registry)에 로그인
- Docker 이미지 업로드 권한 제공
- 배포 시 이미지 다운로드 권한 제공

---

## 서버 파일 설정

### 1. 프로젝트 디렉토리 생성

```bash
mkdir -p ~/myauth
cd ~/myauth
```

### 2. 필요한 파일 업로드

**방법 1: scp 사용 (로컬 PC에서 실행)**
```bash
scp -i /path/to/your-key.pem docker-compose.prod.yml ec2-user@13.124.241.22:~/myauth/
scp -i /path/to/your-key.pem .env.example ec2-user@13.124.241.22:~/myauth/
```

**방법 2: 서버에서 직접 생성**
```bash
# SSH 접속 상태에서
cd ~/myauth
vi docker-compose.prod.yml
# (프로젝트의 docker-compose.prod.yml 내용 붙여넣기)

vi .env.example
# (프로젝트의 .env.example 내용 붙여넣기)
```

### 3. .env 파일 생성 및 설정

```bash
# .env 파일 생성
cp .env.example .env
vi .env
```

**.env 파일 필수 설정 항목:**

```bash
# GitHub 사용자명 (필수)
GITHUB_USERNAME=your-github-username

# MySQL 설정 (필수)
MYSQL_DATABASE=myauth
MYSQL_ROOT_PASSWORD=강력한비밀번호123!

# JWT 설정 (필수 - 64자 이상)
JWT_SECRET=최소64자이상의랜덤문자열
JWT_ACCESS_TOKEN_EXPIRATION=3600000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# 카카오 OAuth (필수)
KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_CLIENT_SECRET=your_kakao_client_secret
KAKAO_REDIRECT_URI=http://13.124.241.22/auth/kakao/callback
```

**안전한 비밀번호 생성 방법:**
```bash
# JWT Secret 생성
openssl rand -base64 64

# MySQL 비밀번호 생성
openssl rand -base64 32
```

---

## 배포 실행

### 자동 배포 (권장)

main 브랜치에 푸시하면 GitHub Actions가 자동으로 배포합니다.

```bash
# 로컬 PC에서
git add .
git commit -m "배포 준비 완료"
git push origin main
```

**배포 과정 모니터링:**
1. GitHub Repository → Actions 탭
2. 워크플로우 실행 상태 확인
3. 빌드 → Docker 이미지 생성 → GHCR 푸시 → Lightsail 배포

### 수동 배포

GitHub Actions 없이 서버에서 직접 배포:

```bash
# Lightsail 서버에서
cd ~/myauth

# GHCR 로그인
echo "YOUR_GITHUB_TOKEN" | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin

# 최신 이미지 다운로드
docker-compose -f docker-compose.prod.yml pull backend

# 컨테이너 시작
docker-compose -f docker-compose.prod.yml up -d

# 상태 확인
docker-compose -f docker-compose.prod.yml ps
```

---

## 배포 확인

### 1. 컨테이너 상태 확인

```bash
# 실행 중인 컨테이너 확인
docker ps

# Docker Compose 상태
cd ~/myauth
docker-compose -f docker-compose.prod.yml ps
```

### 2. 로그 확인

```bash
# 전체 로그 (실시간)
docker-compose -f docker-compose.prod.yml logs -f

# 백엔드만
docker-compose -f docker-compose.prod.yml logs -f backend

# 마지막 100줄
docker-compose -f docker-compose.prod.yml logs --tail=100 backend
```

### 3. API 테스트

```bash
# Health Check
curl http://13.124.241.22/health

# 회원가입 테스트
curl -X POST http://13.124.241.22/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "test1234",
    "nickname": "테스트사용자"
  }'
```

### 4. MySQL 접속

```bash
# MySQL 컨테이너 접속
docker exec -it prod-mysql mysql -u root -p

# 데이터 확인
USE myauth;
SHOW TABLES;
SELECT * FROM users;
EXIT;
```

---

## 문제 해결

### 1. GitHub Actions 빌드 실패

**Gradle 빌드 오류:**
```bash
# 로컬에서 빌드 테스트
./gradlew clean build -x test
```

**Docker 이미지 빌드 실패:**
- Dockerfile 문법 확인
- `build/libs/myauth-0.0.1-SNAPSHOT.jar` 존재 확인

### 2. SSH 배포 실패

**원인:** SSH 연결 문제
- GitHub Secrets의 `LIGHTSAIL_SSH_KEY` 확인
- Lightsail 방화벽에서 SSH(22) 포트 개방 확인

**권한 문제:**
```bash
# 서버에서
ls -la ~/myauth/
chmod 755 ~/myauth/
```

### 3. Docker 이미지 Pull 실패

**GHCR 인증 실패:**
```bash
# Personal Access Token 필요
# GitHub → Settings → Developer settings → Personal access tokens
# 권한: write:packages, read:packages

docker login ghcr.io -u YOUR_GITHUB_USERNAME
```

**이미지가 Private:**
- GitHub Repository → Packages → 이미지 선택
- "Change visibility" → Public으로 변경

### 4. 컨테이너 실행 실패

**MySQL 시작 안됨:**
```bash
# 로그 확인
docker-compose -f docker-compose.prod.yml logs mysql

# 데이터 볼륨 초기화 (주의: 데이터 삭제됨)
docker-compose -f docker-compose.prod.yml down -v
docker-compose -f docker-compose.prod.yml up -d
```

**Backend가 계속 재시작:**
```bash
# 로그 확인
docker-compose -f docker-compose.prod.yml logs --tail=200 backend

# 일반적 원인:
# 1. DB 연결 실패 → .env의 MYSQL_ROOT_PASSWORD 확인
# 2. JWT_SECRET 누락 → .env 파일 확인
# 3. 포트 충돌 → 9080 포트 사용 확인
```

### 5. 포트 문제

```bash
# 포트 사용 중인 프로세스 확인
sudo lsof -i :80

# 프로세스 종료
sudo kill -9 <PID>
```

### 6. 디스크 공간 부족

```bash
# 디스크 사용량 확인
df -h

# Docker 이미지 정리
docker system prune -a

# 볼륨 정리
docker volume prune
```

---

## 추가 명령어

### 컨테이너 관리

```bash
# 중지
docker-compose -f docker-compose.prod.yml stop

# 시작
docker-compose -f docker-compose.prod.yml start

# 재시작
docker-compose -f docker-compose.prod.yml restart

# 완전 삭제 후 재시작
docker-compose -f docker-compose.prod.yml down
docker-compose -f docker-compose.prod.yml up -d
```

### 환경 변수 변경

```bash
# .env 수정
vi .env

# 변경사항 적용
docker-compose -f docker-compose.prod.yml up -d
```

### 데이터 백업

```bash
# MySQL 백업
docker exec prod-mysql mysqldump -u root -p myauth > backup_$(date +%Y%m%d).sql

# 백업 파일 다운로드 (로컬 PC에서)
scp -i /path/to/your-key.pem ec2-user@13.124.241.22:~/backup_*.sql ./
```

---

## 보안 권장사항

1. **SSH 키 관리**
   - 공개 저장소에 절대 커밋 금지
   - GitHub Secrets에만 저장

2. **환경 변수**
   - `.env` 파일은 `.gitignore`에 추가
   - 강력한 비밀번호 사용

3. **방화벽**
   - 필요한 포트만 개방
   - SSH는 특정 IP만 허용 권장

4. **HTTPS 설정 (선택)**
   - 도메인이 있다면 Let's Encrypt SSL 인증서 발급
   - Nginx 리버스 프록시로 HTTPS 적용

---

## 참고 자료

- [AWS Lightsail 공식 문서](https://aws.amazon.com/ko/lightsail/)
- [GitHub Actions 문서](https://docs.github.com/en/actions)
- [Docker Compose 문서](https://docs.docker.com/compose/)
- [GHCR 문서](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)
