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
5. [GitHub Repository 준비 (배포 파일 확인)](#5-github-repository-준비-배포-파일-확인)
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
    │ IP: 15.165.81.224        │
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
- **IP 주소:** 15.165.81.224 (고정 IP)
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
- [ ] **Lightsail IP 주소** 확인 (예: 15.165.81.224)

#### 권장 항목

- [ ] **도메인** (선택사항, 없으면 IP 주소 사용)
- [ ] **카카오 개발자 계정** (OAuth 로그인용)

### 1. AWS Lightsail 인스턴스

#### 필요한 사양
- **IP 주소:** 15.165.81.224 (고정 IP)
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
ssh -i ~/.ssh/LightsailDefaultKey-ap-northeast-2.pem ec2-user@15.165.81.224

# Ubuntu
ssh -i ~/.ssh/LightsailDefaultKey-ap-northeast-2.pem ubuntu@15.165.81.224

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
   http://15.165.81.224/auth/kakao/callback
   ```

---

## 3. Lightsail 서버 초기 설정

이 섹션에서는 새로 만든 Lightsail 서버에 Docker와 Docker Compose를 설치합니다.

### 단계 1: SSH로 서버 접속하기

#### 접속 명령어

**Amazon Linux 2023의 경우:**
```bash
ssh -i ~/.ssh/LightsailDefaultKey-ap-northeast-2.pem ec2-user@15.165.81.224
```

**Ubuntu의 경우:**
```bash
ssh -i ~/.ssh/LightsailDefaultKey-ap-northeast-2.pem ubuntu@15.165.81.224
```

#### 명령어 설명
- `ssh`: 원격 서버에 안전하게 접속하는 명령
- `-i ~/.ssh/LightsailDefaultKey-ap-northeast-2.pem`: 인증에 사용할 SSH 키 파일 지정
- `ec2-user` 또는 `ubuntu`: 서버 접속 사용자명
- `15.165.81.224`: 서버 IP 주소

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
# usermod 사용자 계정 속성 변경 명령어
# -a append, 기존 그룹 유지하면서 추가
# -G groups, 보조 그룹 지정
# docker, 추가할 그룹명
# $USER, 현재 로그인한 사용자명 환경변수(예: ec2-user) 
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
ssh -i ~/.ssh/LightsailDefaultKey-ap-northeast-2.pem ec2-user@15.165.81.224
```

#### Docker 작동 테스트
```bash
# sudo 없이 docker 명령이 실행되는지 확인
docker ps
# 출력: CONTAINER ID   IMAGE   ...  (빈 목록이면 정상)
```

### 단계 4: Docker Compose 설치 (V2 플러그인)

Docker Compose V2는 Docker의 플러그인으로 설치됩니다.

```bash
# 1. Docker Compose 플러그인 디렉토리 생성
sudo mkdir -p /usr/libexec/docker/cli-plugins

# 2. 최신 버전 다운로드 및 설치
sudo curl -SL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-$(uname -m)" \
  -o /usr/libexec/docker/cli-plugins/docker-compose

# 3. 실행 권한 부여
sudo chmod +x /usr/libexec/docker/cli-plugins/docker-compose

# 4. 설치 확인 (하이픈 없이 사용!)
docker compose version
# 출력 예: Docker Compose version v2.24.0
```

#### 명령어 설명
- `mkdir -p /usr/libexec/docker/cli-plugins`: Docker 플러그인 디렉토리 생성
- `curl -SL`: URL에서 파일 다운로드 (-S: 에러 표시, -L: 리다이렉션 따라가기)
- `$(uname -m)`: 현재 CPU 아키텍처 (x86_64 또는 aarch64)
- `-o /usr/libexec/docker/cli-plugins/docker-compose`: 플러그인 경로에 저장
- `chmod +x`: 파일을 실행 가능하게 만들기

#### 중요: 명령어 변경사항
- ❌ 이전 방식: `docker-compose` (하이픈 있음)
- ✅ 새로운 방식: `docker compose` (하이픈 없음)

#### 설치 문제 해결
만약 `docker compose` 명령이 작동하지 않으면:
```bash
# 플러그인 파일 확인
ls -la /usr/libexec/docker/cli-plugins/docker-compose

# 권한 확인 및 수정
sudo chmod +x /usr/libexec/docker/cli-plugins/docker-compose

# Docker 서비스 재시작
sudo systemctl restart docker

# 다시 확인
docker compose version
```

### ✅ 설치 완료 확인

모든 설치가 완료되었는지 확인합니다:

```bash
# Docker 버전 확인
docker --version

# Docker Compose 버전 확인 (하이픈 없이!)
docker compose version

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

### 추가해야 할 Secrets (총 8개)

아래 8개의 Secret을 하나씩 추가합니다.

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
   15.165.81.224
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

#### Secret 4: DB_USERNAME

MySQL 데이터베이스 사용자명을 저장합니다.

**설정 방법:**
1. **New repository secret** 클릭
2. **Name** 입력란:
   ```
   DB_USERNAME
   ```
3. **Secret** 입력란:
   ```
   root
   ```
4. **Add secret** 클릭

**참고:**
- MySQL 컨테이너의 기본 관리자 계정은 `root`입니다
- 프로덕션에서는 별도 사용자 생성을 권장하지만, 초기 배포는 root로 진행합니다

---

#### Secret 5: DB_PASSWORD

MySQL root 계정의 비밀번호를 저장합니다.

**설정 방법:**
1. **New repository secret** 클릭
2. **Name** 입력란:
   ```
   DB_PASSWORD
   ```
3. **Secret** 입력란에 **강력한 비밀번호** 입력:
   ```
   예: MyS3cur3P@ssw0rd!2024
   ```
4. **Add secret** 클릭

**강력한 비밀번호 생성 방법:**
```bash
# 터미널에서 랜덤 비밀번호 생성
openssl rand -base64 32
```

**주의사항:**
- 최소 12자 이상
- 대소문자, 숫자, 특수문자 조합
- 이 비밀번호는 MySQL 컨테이너 생성 시 root 계정 비밀번호로 설정됩니다

---

#### Secret 6: JWT_SECRET

JWT 토큰 서명에 사용할 비밀 키를 저장합니다.

**설정 방법:**
1. **New repository secret** 클릭
2. **Name** 입력란:
   ```
   JWT_SECRET
   ```
3. **Secret** 입력란에 **최소 64자 이상의 랜덤 문자열** 입력:
   ```
   예: openssl rand -base64 64 명령으로 생성한 값
   ```
4. **Add secret** 클릭

**JWT Secret 생성 방법:**
```bash
# 터미널에서 안전한 JWT Secret 생성
openssl rand -base64 64
```

**경고:**
- 이 키가 노출되면 누구나 인증 토큰을 위조할 수 있습니다!
- 절대로 코드에 직접 작성하거나 공개하지 마세요

---

#### Secret 7: KAKAO_CLIENT_ID

카카오 개발자 콘솔에서 발급받은 REST API 키를 저장합니다.

**설정 방법:**
1. **New repository secret** 클릭
2. **Name** 입력란:
   ```
   KAKAO_CLIENT_ID
   ```
3. **Secret** 입력란에 **카카오 REST API 키** 입력:
   ```
   예: f0bfa98dfa477735feeb8dbfdfa1d105
   ```
4. **Add secret** 클릭

**카카오 REST API 키 확인:**
1. https://developers.kakao.com/ 접속
2. 내 애플리케이션 선택
3. 앱 키 → REST API 키 복사

---

#### Secret 8: KAKAO_CLIENT_SECRET

카카오 Client Secret을 저장합니다.

**설정 방법:**
1. **New repository secret** 클릭
2. **Name** 입력란:
   ```
   KAKAO_CLIENT_SECRET
   ```
3. **Secret** 입력란에 **카카오 Client Secret** 입력:
   ```
   예: U4JgTAZGirCvVGmmnvuSlsoWlKFPstvV
   ```
4. **Add secret** 클릭

**카카오 Client Secret 확인:**
1. https://developers.kakao.com/ 접속
2. 내 애플리케이션 선택
3. 보안 탭 → Client Secret 활성화 및 코드 생성
4. 생성된 코드 복사

---

### ✅ Secret 설정 완료 확인

8개의 Secret이 모두 추가되었는지 확인합니다.

GitHub → Settings → Secrets and variables → Actions 페이지에서 다음 8개가 보여야 합니다:

```
DB_PASSWORD
DB_USERNAME
JWT_SECRET
KAKAO_CLIENT_ID
KAKAO_CLIENT_SECRET
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

## 5. GitHub Repository 준비 (배포 파일 확인)

### GitHub Actions 자동 배포의 동작 원리

이 프로젝트는 **완전 자동화된 배포 시스템**을 사용합니다. 모든 설정 파일과 환경 변수는 GitHub Actions가 자동으로 처리합니다.

### 자동 배포 프로세스

```
┌──────────────────────────────────────────────────────┐
│  1. git push origin main                             │
│     (코드를 GitHub에 푸시)                            │
└────────────────┬─────────────────────────────────────┘
                 ↓
┌──────────────────────────────────────────────────────┐
│  2. GitHub Actions 자동 실행                         │
│     - Spring Boot 빌드                               │
│     - Docker 이미지 생성 및 GHCR 업로드              │
└────────────────┬─────────────────────────────────────┘
                 ↓
┌──────────────────────────────────────────────────────┐
│  3. Lightsail 서버 자동 배포                         │
│     ✅ Git clone (docker-compose.prod.yml 다운로드)  │
│     ✅ .env 파일 자동 생성 (GitHub Secrets 사용)    │
│     ✅ 최신 Docker 이미지 다운로드                   │
│     ✅ 컨테이너 자동 재시작                          │
└──────────────────────────────────────────────────────┘
```

### 필요한 파일 (이미 Git에 포함되어 있음)

다음 파일들이 GitHub 저장소에 포함되어 있어야 합니다:

- [x] **`docker-compose.prod.yml`** (프로젝트 루트)
  - 프로덕션 환경 컨테이너 설정
  - 환경 변수 참조만 포함 (민감 정보 없음)

- [x] **`.env.example`** (프로젝트 루트)
  - 환경 변수 템플릿
  - 실제 값이 아닌 예시만 포함

- [x] **`.github/workflows/deploy.yml`**
  - GitHub Actions 자동 배포 설정
  - `.env` 파일 자동 생성 로직 포함

### GitHub에서 파일 확인

GitHub 저장소에서 다음 파일들이 존재하는지 확인하세요:

```bash
# 로컬에서 확인
ls -la docker-compose.prod.yml
ls -la .env.example
ls -la .github/workflows/deploy.yml

# GitHub 웹사이트에서도 확인 가능
```

### 중요: 수동 파일 업로드 불필요!

이전 방식과 달리, **서버에 파일을 수동으로 업로드할 필요가 전혀 없습니다!**

- ❌ scp로 파일 업로드 불필요
- ❌ 서버에서 vi로 파일 생성 불필요
- ❌ .env 파일 수동 작성 불필요

모든 것은 **GitHub Actions가 자동으로 처리**합니다!

---

## 6. 배포 실행하기

### 🚀 자동 배포 (완전 자동화, 권장)

**가장 간단한 배포 방법! 단 한 줄의 명령어만 입력하면 됩니다.**

```bash
# 로컬 PC에서
git add .
git commit -m "프로덕션 배포"
git push origin main
```

이제 끝입니다! GitHub Actions가 자동으로 다음 모든 작업을 수행합니다:

```
✅ 1. Spring Boot 빌드
✅ 2. Docker 이미지 생성
✅ 3. GHCR에 이미지 업로드
✅ 4. Lightsail 서버 접속
✅ 5. 저장소 클론 (docker-compose.prod.yml 다운로드)
✅ 6. .env 파일 자동 생성 (GitHub Secrets 사용)
✅ 7. 최신 이미지 다운로드
✅ 8. 컨테이너 자동 재시작
```

### 📊 배포 과정 모니터링

GitHub에서 배포 진행 상황을 실시간으로 확인할 수 있습니다:

1. **GitHub Repository → Actions 탭** 클릭
2. 최신 워크플로우 실행 확인 (Deploy to Lightsail)
3. 각 단계별 진행 상황 모니터링:
   ```
   ✓ 코드 체크아웃
   ✓ Java 17 설정
   ✓ Gradle 빌드
   ✓ Docker 이미지 빌드
   ✓ GHCR 푸시
   ✓ Lightsail 서버 배포 ← 가장 중요!
   ```
4. 모든 단계가 ✅ 녹색 체크로 표시되면 배포 성공!

### 🔑 중요: .env 파일 자동 생성

GitHub Actions는 배포 시 자동으로 `.env` 파일을 생성합니다:

**`.github/workflows/deploy.yml`의 자동 생성 코드:**
```yaml
# GitHub Secrets의 값을 사용하여 .env 파일 자동 생성
cat > .env << 'EOF'
DB_USERNAME=${{ secrets.DB_USERNAME }}
DB_PASSWORD=${{ secrets.DB_PASSWORD }}
JWT_SECRET=${{ secrets.JWT_SECRET }}
KAKAO_CLIENT_ID=${{ secrets.KAKAO_CLIENT_ID }}
KAKAO_CLIENT_SECRET=${{ secrets.KAKAO_CLIENT_SECRET }}
SPRING_PROFILES_ACTIVE=prod
EOF
```

따라서 **서버에서 .env 파일을 수동으로 작성할 필요가 전혀 없습니다!**

### ⚙️ 수동 배포 (선택사항)

GitHub Actions를 사용하지 않고 서버에서 직접 배포하려면:

```bash
# 1. Lightsail 서버 SSH 접속
ssh -i ~/.ssh/your-key.pem ec2-user@15.165.81.224

# 2. 프로젝트 디렉토리로 이동 (없으면 생성)
mkdir -p ~/myauth && cd ~/myauth

# 3. GitHub 저장소 클론
git clone https://github.com/your-username/myauth.git .

# 4. .env 파일 수동 생성
cat > .env << 'EOF'
DB_USERNAME=root
DB_PASSWORD=your_db_password
JWT_SECRET=your_jwt_secret
JWT_ACCESS_TOKEN_EXPIRATION=3600000
JWT_REFRESH_TOKEN_EXPIRATION=604800000
KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_CLIENT_SECRET=your_kakao_client_secret
KAKAO_REDIRECT_URI=http://15.165.81.224:8080/auth/kakao/callback
SPRING_PROFILES_ACTIVE=prod
EOF

# 5. GHCR 로그인 (GitHub Personal Access Token 필요)
echo "YOUR_GITHUB_TOKEN" | sudo docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin

# 6. 최신 이미지 다운로드
sudo docker compose -f docker-compose.prod.yml pull

# 7. 컨테이너 시작
sudo docker compose -f docker-compose.prod.yml up -d

# 8. 상태 확인
sudo docker compose -f docker-compose.prod.yml ps
```

**주의:** 수동 배포는 복잡하고 실수하기 쉬우므로 **자동 배포를 강력히 권장**합니다!

---

## 7. 배포 확인하기

### 1. 컨테이너 상태 확인

```bash
# 실행 중인 컨테이너 확인
docker ps

# Docker Compose 상태
cd ~/myauth
docker compose -f docker-compose.prod.yml ps
```

### 2. 로그 확인

```bash
# 전체 로그 (실시간)
docker compose -f docker-compose.prod.yml logs -f

# 백엔드만
docker compose -f docker-compose.prod.yml logs -f backend

# 마지막 100줄
docker compose -f docker-compose.prod.yml logs --tail=100 backend
```

### 3. API 테스트

```bash
# Health Check
curl http://15.165.81.224/health

# 회원가입 테스트
curl -X POST http://15.165.81.224/api/auth/signup \
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

## 8. 문제 해결 가이드

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
docker compose -f docker-compose.prod.yml logs mysql

# 데이터 볼륨 초기화 (주의: 데이터 삭제됨)
docker compose -f docker-compose.prod.yml down -v
docker compose -f docker-compose.prod.yml up -d
```

**Backend가 계속 재시작:**
```bash
# 로그 확인
docker compose -f docker-compose.prod.yml logs --tail=200 backend

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
docker compose -f docker-compose.prod.yml stop

# 시작
docker compose -f docker-compose.prod.yml start

# 재시작
docker compose -f docker-compose.prod.yml restart

# 완전 삭제 후 재시작
docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml up -d
```

### 환경 변수 변경

```bash
# .env 수정
vi .env

# 변경사항 적용
docker compose -f docker-compose.prod.yml up -d
```

### 데이터 백업

```bash
# MySQL 백업
docker exec prod-mysql mysqldump -u root -p myauth > backup_$(date +%Y%m%d).sql

# 백업 파일 다운로드 (로컬 PC에서)
scp -i /path/to/your-key.pem ec2-user@15.165.81.224:~/backup_*.sql ./
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

## 프로덕션 배포 최종 체크리스트

배포하기 전에 아래 체크리스트를 모두 확인하여 안전하고 성공적인 배포를 보장하세요.

### ✅ 1. 배포 전 필수 확인사항

#### 1.1 로컬 개발 환경 설정 확인

- [ ] **application-prod.yaml 설정 완료**
  ```yaml
  # src/main/resources/application-prod.yaml에서 확인
  spring:
    jpa:
      hibernate:
        ddl-auto: update  # 반드시 설정되어 있어야 함 (테이블 자동 생성)
  ```
  - 위치: `src/main/resources/application-prod.yaml`
  - 확인 사항: `ddl-auto: update` 설정이 있는지 확인
  - **중요:** 이 설정이 없으면 최초 배포 시 데이터베이스 테이블이 생성되지 않아 애플리케이션이 실행되지 않습니다!

- [ ] **docker-compose.prod.yml 파일 존재 확인**
  - 위치: 프로젝트 루트 디렉토리
  - MySQL 컨테이너 설정 포함 여부
  - Backend 컨테이너 설정 포함 여부
  - 네트워크 및 볼륨 설정 확인

- [ ] **Dockerfile 파일 존재 확인**
  - 위치: 프로젝트 루트 디렉토리
  - Java 17 기반 이미지 사용
  - JAR 파일 복사 설정

- [ ] **.github/workflows/deploy.yml 파일 존재 확인**
  - 위치: `.github/workflows/` 디렉토리
  - GitHub Actions 워크플로우 설정 완료

- [ ] **.gitignore에 .env 파일 포함 확인**
  ```bash
  # .gitignore 파일에서 확인
  cat .gitignore | grep "\.env"
  ```
  - `.env` 파일이 GitHub에 업로드되지 않도록 방지

#### 1.2 GitHub 설정 확인

- [ ] **GitHub Repository 생성 완료**
  - Public 또는 Private 저장소
  - 코드 푸시 완료

- [ ] **GitHub Secrets 8개 모두 설정 완료**

  GitHub → Settings → Secrets and variables → Actions 페이지에서 다음 8개 확인:

  ```
  ✓ DB_PASSWORD              (MySQL root 비밀번호)
  ✓ DB_USERNAME              (MySQL 사용자명: root)
  ✓ JWT_SECRET               (JWT 토큰 서명 키)
  ✓ KAKAO_CLIENT_ID          (카카오 REST API 키)
  ✓ KAKAO_CLIENT_SECRET      (카카오 Client Secret)
  ✓ LIGHTSAIL_HOST           (서버 IP: 15.165.81.224)
  ✓ LIGHTSAIL_SSH_KEY        (SSH 키 전체 내용)
  ✓ LIGHTSAIL_USER           (SSH 사용자: ec2-user 또는 ubuntu)
  ```

- [ ] **GHCR 권한 확인**
  - GitHub Personal Access Token 발급 (필요 시)
  - 권한: `write:packages`, `read:packages`

#### 1.3 AWS Lightsail 서버 설정 확인

- [ ] **Lightsail 인스턴스 생성 완료**
  - 운영체제: Amazon Linux 2023 또는 Ubuntu 22.04
  - 최소 사양: RAM 1GB, vCPU 1코어, 디스크 20GB
  - 고정 IP 주소 연결 완료

- [ ] **방화벽 포트 개방 완료**

  Lightsail 콘솔 → 네트워킹 → 방화벽에서 다음 포트 확인:

  | 포트 | 프로토콜 | 용도 | 상태 |
  |------|----------|------|------|
  | 22 | TCP | SSH | ✓ 개방 필수 |
  | 80 | TCP | HTTP | ✓ 개방 필수 |
  | 443 | TCP | HTTPS | ○ 권장 |
  | 8080 | TCP | Backend API | ✓ 개방 필수 |

- [ ] **SSH 키 다운로드 및 안전 보관**
  - SSH 키 파일 (.pem) 다운로드 완료
  - 권한 설정 완료: `chmod 400 your-key.pem`
  - 안전한 위치에 보관 (절대 GitHub에 업로드 금지)

- [ ] **SSH 접속 테스트 성공**
  ```bash
  # Amazon Linux 2023
  ssh -i ~/.ssh/your-key.pem ec2-user@15.165.81.224

  # Ubuntu
  ssh -i ~/.ssh/your-key.pem ubuntu@15.165.81.224
  ```

#### 1.4 Lightsail 서버 환경 설정 확인

SSH로 서버에 접속하여 다음 항목들을 확인합니다:

- [ ] **시스템 업데이트 완료**
  ```bash
  # Amazon Linux 2023
  sudo dnf update -y

  # Ubuntu
  sudo apt update && sudo apt upgrade -y
  ```

- [ ] **Docker 설치 및 실행 확인**
  ```bash
  # Docker 버전 확인
  docker --version

  # Docker 서비스 상태 확인
  sudo systemctl status docker

  # sudo 없이 docker 명령 실행 가능 확인
  docker ps
  ```

- [ ] **Docker Compose 설치 확인**
  ```bash
  # Docker Compose 버전 확인
  docker-compose --version
  # 출력 예: Docker Compose version v2.24.0
  ```

- [ ] **현재 사용자가 docker 그룹에 포함되어 있는지 확인**
  ```bash
  # 그룹 확인
  groups
  # 출력에 "docker"가 포함되어 있어야 함
  ```

  만약 포함되어 있지 않다면:
  ```bash
  # docker 그룹에 추가
  sudo usermod -aG docker $USER

  # SSH 재접속 필요
  exit
  ssh -i ~/.ssh/your-key.pem ec2-user@15.165.81.224
  ```

#### 1.5 카카오 OAuth 설정 확인 (선택사항)

- [ ] **카카오 개발자 콘솔 설정 완료**
  - 애플리케이션 생성 완료
  - REST API 키 발급 완료
  - Client Secret 발급 및 활성화 완료

- [ ] **Redirect URI 등록 완료**

  카카오 개발자 콘솔 → 내 애플리케이션 → 앱 설정 → 플랫폼 → Web → Redirect URI에 추가:
  ```
  http://15.165.81.224:8080/auth/kakao/callback
  ```

---

### ✅ 2. 배포 실행 체크리스트

#### 2.1 로컬에서 코드 푸시

- [ ] **모든 변경사항 커밋 및 푸시**
  ```bash
  # 변경사항 확인
  git status

  # 변경사항 스테이징
  git add .

  # 커밋
  git commit -m "프로덕션 배포 준비 완료"

  # GitHub에 푸시
  git push origin main
  ```

#### 2.2 GitHub Actions 워크플로우 모니터링

- [ ] **GitHub Actions 실행 확인**
  - GitHub Repository → Actions 탭 클릭
  - 최신 워크플로우 실행 확인

- [ ] **빌드 단계 성공 확인**

  워크플로우에서 다음 단계들이 모두 성공해야 합니다:

  ```
  ✓ 1. Checkout code
  ✓ 2. Set up Java 17
  ✓ 3. Grant execute permission for gradlew
  ✓ 4. Build with Gradle
  ✓ 5. Log in to GHCR
  ✓ 6. Build Docker image
  ✓ 7. Push Docker image to GHCR
  ✓ 8. Deploy to Lightsail
  ```

- [ ] **Docker 이미지 GHCR 업로드 성공 확인**
  - GitHub Repository → Packages 탭
  - `ghcr.io/your-username/myauth:latest` 이미지 존재 확인

#### 2.3 배포 과정 오류 확인

배포 중 오류가 발생하면:

- [ ] **GitHub Actions 로그 확인**
  - Actions 탭 → 실패한 워크플로우 클릭
  - 어느 단계에서 실패했는지 확인
  - 오류 메시지 읽고 문제 해결

- [ ] **일반적인 배포 실패 원인**
  - ❌ Gradle 빌드 실패 → 로컬에서 `./gradlew clean build` 테스트
  - ❌ Docker 이미지 빌드 실패 → Dockerfile 문법 확인
  - ❌ GHCR 인증 실패 → GITHUB_TOKEN 권한 확인
  - ❌ SSH 접속 실패 → LIGHTSAIL_SSH_KEY Secret 확인
  - ❌ 서버 배포 실패 → Lightsail 서버 상태 및 Docker 설치 확인

---

### ✅ 3. 배포 후 검증 체크리스트

SSH로 Lightsail 서버에 접속하여 다음 항목들을 확인합니다.

#### 3.1 컨테이너 실행 상태 확인

- [ ] **실행 중인 컨테이너 확인**
  ```bash
  # 서버에서 실행
  cd ~/myauth
  docker compose -f docker-compose.prod.yml ps
  ```

  출력에서 다음 2개 컨테이너가 모두 "Up" 상태여야 합니다:
  ```
  NAME              STATUS
  prod-mysql        Up (healthy)
  prod-backend      Up
  ```

- [ ] **컨테이너 상세 정보 확인**
  ```bash
  # 모든 컨테이너 상태
  docker ps
  ```

  확인 사항:
  - `prod-mysql`: 포트 3306 열림, STATUS가 "Up" 또는 "Up (healthy)"
  - `prod-backend`: 포트 8080→9080 매핑, STATUS가 "Up"

#### 3.2 로그 확인

- [ ] **MySQL 컨테이너 로그 확인**
  ```bash
  # MySQL 로그 확인
  docker compose -f docker-compose.prod.yml logs mysql
  ```

  정상 메시지 예시:
  ```
  [Server] /usr/sbin/mysqld: ready for connections.
  [Server] Version: '8.0.xx'  socket: '/var/run/mysqld/mysqld.sock'  port: 3306
  ```

- [ ] **Backend 컨테이너 로그 확인**
  ```bash
  # Backend 로그 확인 (실시간)
  docker compose -f docker-compose.prod.yml logs -f backend
  ```

  정상 메시지 예시:
  ```
  Started MyAuthApplication in X.XXX seconds
  Tomcat started on port(s): 9080 (http)
  ```

  확인 사항:
  - Spring Boot 애플리케이션이 정상 시작되었는지
  - 데이터베이스 연결 성공 메시지
  - 포트 9080에서 Tomcat 실행 중
  - 에러 메시지가 없는지

#### 3.3 데이터베이스 테이블 자동 생성 확인

- [ ] **MySQL 컨테이너 접속 및 테이블 확인**
  ```bash
  # MySQL 컨테이너에 접속
  docker exec -it prod-mysql mysql -u root -p
  # 비밀번호 입력: GitHub Secret의 DB_PASSWORD 값 입력
  ```

  MySQL 접속 후:
  ```sql
  -- 데이터베이스 선택
  USE mannal;

  -- 테이블 목록 확인
  SHOW TABLES;
  ```

  출력 예시 (JPA 엔티티에 따라 다를 수 있음):
  ```
  +------------------+
  | Tables_in_mannal |
  +------------------+
  | users            |
  | refresh_tokens   |
  | ...기타 테이블    |
  +------------------+
  ```

  확인 사항:
  - `users` 테이블이 자동으로 생성되었는지
  - 기타 엔티티 테이블들이 모두 생성되었는지
  - **중요:** 테이블이 생성되지 않았다면 `application-prod.yaml`의 `ddl-auto: update` 설정 확인!

  ```sql
  -- 테이블 구조 확인 (예: users 테이블)
  DESCRIBE users;

  -- MySQL 종료
  EXIT;
  ```

#### 3.4 API 엔드포인트 테스트

- [ ] **Health Check 엔드포인트 테스트**
  ```bash
  # 서버 또는 로컬 PC에서 실행
  curl http://15.165.81.224:8080/health
  ```

  예상 응답: `200 OK` 또는 헬스 체크 JSON 응답

- [ ] **회원가입 API 테스트**
  ```bash
  curl -X POST http://15.165.81.224:8080/api/auth/signup \
    -H "Content-Type: application/json" \
    -d '{
      "email": "test@example.com",
      "password": "test1234",
      "nickname": "테스트사용자"
    }'
  ```

  예상 응답: 회원가입 성공 JSON 또는 사용자 정보

- [ ] **로그인 API 테스트**
  ```bash
  curl -X POST http://15.165.81.224:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{
      "email": "test@example.com",
      "password": "test1234"
    }'
  ```

  예상 응답: JWT 토큰 포함된 JSON

#### 3.5 카카오 OAuth 로그인 테스트 (선택사항)

- [ ] **카카오 로그인 리다이렉트 URL 테스트**

  웹 브라우저에서 접속:
  ```
  http://15.165.81.224:8080/oauth2/authorization/kakao
  ```

  확인 사항:
  - 카카오 로그인 페이지로 정상 리다이렉트되는지
  - 로그인 후 백엔드로 콜백이 정상 작동하는지
  - 토큰이 정상 발급되는지

---

### ✅ 4. 보안 점검 체크리스트

#### 4.1 코드 보안 확인

- [ ] **.env 파일이 Git에 커밋되지 않았는지 확인**
  ```bash
  # .gitignore 확인
  cat .gitignore | grep "\.env"

  # Git 히스토리에 .env 파일이 없는지 확인
  git log --all --full-history -- .env
  # 출력이 없어야 정상
  ```

- [ ] **application-prod.yaml에 민감한 정보가 하드코딩되지 않았는지 확인**
  - 데이터베이스 비밀번호
  - JWT Secret
  - 카카오 Client Secret

  모든 민감한 정보는 환경 변수로 처리되어야 합니다!

- [ ] **GitHub Secrets가 코드에 노출되지 않았는지 확인**
  ```bash
  # 코드 전체에서 비밀번호 검색
  grep -r "MyS3cur3P@ssw0rd" .
  # 출력이 없어야 정상
  ```

#### 4.2 서버 보안 확인

- [ ] **SSH 키 파일 권한 확인**
  ```bash
  # 로컬 PC에서
  ls -l ~/.ssh/your-key.pem
  # 출력: -r-------- 또는 -rw------- (400 또는 600)
  ```

- [ ] **서버 방화벽 설정 확인**
  - Lightsail 콘솔에서 필요한 포트만 개방되었는지 확인
  - 불필요한 포트는 차단

- [ ] **.env 파일 권한 확인 (서버)**
  ```bash
  # 서버에서
  ls -la ~/myauth/.env
  # 출력: -rw------- (600) - 소유자만 읽기/쓰기 가능

  # 권한이 잘못되었다면 수정
  chmod 600 ~/myauth/.env
  ```

#### 4.3 비밀번호 강도 확인

- [ ] **DB_PASSWORD 강도 확인**
  - 최소 12자 이상
  - 대소문자, 숫자, 특수문자 조합
  - 예측 가능한 단어 사용 금지

- [ ] **JWT_SECRET 강도 확인**
  - 최소 64자 이상
  - 랜덤 문자열 (openssl rand -base64 64로 생성)

---

### ✅ 5. 데이터 백업 체크리스트 (배포 후 권장)

#### 5.1 MySQL 데이터 백업

- [ ] **MySQL 백업 스크립트 작성**
  ```bash
  # 서버에서 백업 디렉토리 생성
  mkdir -p ~/backups

  # MySQL 백업 스크립트 생성
  cat > ~/backup-mysql.sh << 'EOF'
  #!/bin/bash
  BACKUP_DIR=~/backups
  DATE=$(date +%Y%m%d_%H%M%S)
  docker exec prod-mysql mysqldump -u root -p${DB_PASSWORD} mannal > $BACKUP_DIR/mysql_backup_$DATE.sql
  echo "백업 완료: $BACKUP_DIR/mysql_backup_$DATE.sql"
  EOF

  # 실행 권한 부여
  chmod +x ~/backup-mysql.sh
  ```

- [ ] **수동 백업 테스트**
  ```bash
  # 백업 실행
  ~/backup-mysql.sh

  # 백업 파일 확인
  ls -lh ~/backups/
  ```

- [ ] **자동 백업 스케줄 설정 (Cron)**
  ```bash
  # Cron 편집
  crontab -e

  # 매일 새벽 3시에 백업 (아래 라인 추가)
  0 3 * * * /home/ec2-user/backup-mysql.sh
  ```

#### 5.2 백업 파일 다운로드

- [ ] **로컬 PC로 백업 파일 다운로드**
  ```bash
  # 로컬 PC에서 실행
  scp -i ~/.ssh/your-key.pem ec2-user@15.165.81.224:~/backups/mysql_backup_*.sql ./
  ```

---

### ✅ 6. 모니터링 및 유지보수 체크리스트

#### 6.1 일상 모니터링

- [ ] **디스크 사용량 확인**
  ```bash
  # 서버에서
  df -h
  ```

  확인 사항:
  - 디스크 사용량이 80% 이하인지
  - Docker 볼륨이 과도하게 커지지 않았는지

- [ ] **컨테이너 상태 정기 확인**
  ```bash
  # 서버에서
  docker ps
  docker stats --no-stream
  ```

- [ ] **로그 파일 크기 확인**
  ```bash
  # Docker 로그 크기 확인
  docker compose -f docker-compose.prod.yml logs --tail=0 2>&1 | wc -l
  ```

#### 6.2 업데이트 및 재배포

- [ ] **새 버전 배포 시 체크리스트**
  - 로컬에서 테스트 완료
  - 변경 사항 커밋 및 푸시
  - GitHub Actions 워크플로우 성공 확인
  - 서버에서 컨테이너 재시작 확인
  - API 테스트 재실행

---

### 🎯 최종 배포 완료 확인

모든 체크리스트를 완료했다면 다음 항목들이 모두 작동해야 합니다:

```
✅ Lightsail 서버가 실행 중
✅ MySQL 컨테이너 실행 중 (healthy 상태)
✅ Backend 컨테이너 실행 중
✅ 데이터베이스 테이블 자동 생성 완료
✅ API 엔드포인트 정상 응답
✅ 카카오 OAuth 로그인 작동 (선택사항)
✅ GitHub Actions 자동 배포 작동
✅ 로그에 에러 메시지 없음
✅ 보안 설정 완료 (.env 파일 보호, SSH 키 안전 보관)
```

**축하합니다! 프로덕션 배포가 성공적으로 완료되었습니다! 🎉**

---

## 참고 자료

- [AWS Lightsail 공식 문서](https://aws.amazon.com/ko/lightsail/)
- [GitHub Actions 문서](https://docs.github.com/en/actions)
- [Docker Compose 문서](https://docs.docker.com/compose/)
- [GHCR 문서](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)
