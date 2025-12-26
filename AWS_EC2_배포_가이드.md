# AWS EC2 Amazon Linux 2023 배포 가이드

React + Spring Boot + MySQL 프로젝트를 AWS EC2에 Docker로 배포하는 완전한 가이드입니다.

## 목차
1. [사전 준비](#1-사전-준비)
2. [EC2 인스턴스 생성](#2-ec2-인스턴스-생성)
3. [서버 초기 설정](#3-서버-초기-설정)
4. [Docker 설치](#4-docker-설치)
5. [프로젝트 파일 준비](#5-프로젝트-파일-준비)
6. [React 프론트엔드 빌드](#6-react-프론트엔드-빌드)
7. [환경 변수 설정](#7-환경-변수-설정)
8. [Docker Compose로 배포](#8-docker-compose로-배포)
9. [도메인 연결 (선택)](#9-도메인-연결-선택)
10. [SSL/HTTPS 설정 (선택)](#10-sslhttps-설정-선택)
11. [문제 해결](#11-문제-해결)

---

## 1. 사전 준비

### 필요한 것들
- AWS 계정
- 도메인 (선택사항, IP로도 접속 가능)
- SSH 클라이언트 (Mac/Linux는 기본 제공, Windows는 PuTTY 또는 Git Bash)

### 로컬에서 준비할 것
```bash
# 1. 프로젝트 클론 또는 압축
cd /path/to/myauth
tar -czf myauth.tar.gz .

# 2. React 프론트엔드 빌드 (로컬에서 미리 하는 것을 추천)
cd /path/to/frontend
npm run build
```

---

## 2. EC2 인스턴스 생성

### 2.1 AWS 콘솔 접속
1. [AWS Console](https://console.aws.amazon.com) 접속
2. EC2 서비스로 이동
3. "인스턴스 시작" 클릭

### 2.2 인스턴스 설정
| 항목 | 추천 설정 |
|------|----------|
| **이름** | myauth-production |
| **AMI** | Amazon Linux 2023 AMI |
| **인스턴스 유형** | t2.small 또는 t3.small (최소 1GB RAM) |
| **키 페어** | 새로 생성 또는 기존 키 사용 (.pem 파일 다운로드 및 보관) |
| **네트워크** | 기본 VPC |
| **스토리지** | 20GB gp3 |

### 2.3 보안 그룹 설정
다음 포트를 열어야 합니다:

| 유형 | 포트 | 소스 | 설명 |
|------|------|------|------|
| SSH | 22 | My IP | SSH 접속용 (본인 IP만 허용) |
| HTTP | 80 | 0.0.0.0/0 | 웹 접속용 |
| HTTPS | 443 | 0.0.0.0/0 | HTTPS 접속용 (SSL 사용 시) |
| Custom TCP | 9080 | 0.0.0.0/0 | 백엔드 직접 접속용 (선택, 테스트용) |

### 2.4 탄력적 IP 할당 (선택, 권장)
인스턴스 재시작 시 IP가 변경되지 않도록 고정 IP 할당:
1. EC2 콘솔 → 탄력적 IP → "탄력적 IP 주소 할당"
2. 할당된 IP를 인스턴스와 연결

---

## 3. 서버 초기 설정

### 3.1 SSH 접속
```bash
# Mac/Linux
chmod 400 your-key.pem
ssh -i your-key.pem ec2-user@your-ec2-ip

# Windows (Git Bash 또는 PowerShell)
ssh -i your-key.pem ec2-user@your-ec2-ip
```

### 3.2 시스템 업데이트
```bash
sudo dnf update -y
```

### 3.3 필수 도구 설치
```bash
# Git 설치
sudo dnf install -y git

# Vim 또는 Nano 설치 (텍스트 에디터)
sudo dnf install -y vim
```

---

## 4. Docker 설치

### 4.1 Docker 설치
```bash
# Docker 설치
sudo dnf install -y docker

# Docker 서비스 시작
sudo systemctl start docker

# 부팅 시 자동 시작 설정
sudo systemctl enable docker

# 현재 사용자를 docker 그룹에 추가 (sudo 없이 사용 가능)
sudo usermod -aG docker $USER

# 로그아웃 후 재로그인하여 그룹 권한 적용
exit
ssh -i your-key.pem ec2-user@your-ec2-ip

# Docker 버전 확인
docker --version
```

### 4.2 Docker Compose 설치
```bash
# Docker Compose 다운로드
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose

# 실행 권한 부여
sudo chmod +x /usr/local/bin/docker-compose

# 버전 확인
docker-compose --version
```

---

## 5. 프로젝트 파일 준비

### 방법 1: Git Clone (권장)
```bash
cd ~
git clone https://github.com/your-username/myauth.git
cd myauth
```

### 방법 2: SCP로 파일 전송
```bash
# 로컬에서 실행
scp -i your-key.pem myauth.tar.gz ec2-user@your-ec2-ip:~

# 서버에서 압축 해제
ssh -i your-key.pem ec2-user@your-ec2-ip
cd ~
tar -xzf myauth.tar.gz
cd myauth
```

---

## 6. React 프론트엔드 빌드

### 6.1 로컬에서 빌드 (권장)
```bash
# 로컬 컴퓨터에서
cd /path/to/frontend
npm install
npm run build

# 빌드 파일을 서버로 전송
scp -i your-key.pem -r build ec2-user@your-ec2-ip:~/myauth/frontend/
```

### 6.2 서버에서 빌드 (선택)
```bash
# 서버에 Node.js 설치
sudo dnf install -y nodejs npm

# 프론트엔드 빌드
cd ~/myauth/frontend
npm install
npm run build
```

---

## 7. 환경 변수 설정

### 7.1 .env 파일 생성
```bash
cd ~/myauth
vi .env
```

### 7.2 .env 파일 내용
```env
# MySQL 설정
MYSQL_ROOT_PASSWORD=your_strong_password_here_123!@#
MYSQL_DATABASE=myauth

# JWT 설정 (최소 64자 이상 권장)
JWT_SECRET=your_jwt_secret_key_at_least_64_characters_for_HS512_algorithm_change_this_in_production
JWT_ACCESS_TOKEN_EXPIRATION=3600000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# Kakao OAuth (실제 값으로 변경)
KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_CLIENT_SECRET=your_kakao_client_secret
KAKAO_REDIRECT_URI=https://yourdomain.com/auth/kakao/callback
```

**중요:** 모든 비밀번호와 시크릿 키는 강력한 값으로 변경하세요!

### 7.3 환경 변수 보안
```bash
# .env 파일 권한 설정 (소유자만 읽기 가능)
chmod 600 .env
```

---

## 8. Docker Compose로 배포

### 8.1 Nginx 설정 파일 생성
```bash
# Nginx 디렉토리 생성
mkdir -p ~/myauth/nginx/conf.d

# 메인 설정 파일 생성
vi ~/myauth/nginx/nginx.conf
```

**nginx.conf 내용:**
```nginx
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log warn;
pid /var/run/nginx.pid;

events {
    worker_connections 1024;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';

    access_log /var/log/nginx/access.log main;

    sendfile on;
    tcp_nopush on;
    keepalive_timeout 65;
    gzip on;

    include /etc/nginx/conf.d/*.conf;
}
```

**사이트 설정 파일 생성:**
```bash
vi ~/myauth/nginx/conf.d/default.conf
```

**default.conf 내용:**
```nginx
# HTTP 서버 (HTTPS로 리다이렉트)
server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;  # 실제 도메인으로 변경

    # Let's Encrypt SSL 인증용 (HTTPS 설정 시)
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    # 나머지 모든 요청은 HTTPS로 리다이렉트 (HTTPS 설정 시 주석 해제)
    # return 301 https://$server_name$request_uri;

    # HTTPS 설정 전까지는 아래 설정 사용
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # API 요청은 백엔드로 프록시
    location /api/ {
        proxy_pass http://backend:9080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # OAuth 콜백도 백엔드로 프록시
    location /auth/ {
        proxy_pass http://backend:9080/auth/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

# HTTPS 서버 (SSL 인증서 설정 후 사용)
# server {
#     listen 443 ssl http2;
#     server_name yourdomain.com www.yourdomain.com;
#
#     ssl_certificate /etc/nginx/ssl/fullchain.pem;
#     ssl_certificate_key /etc/nginx/ssl/privkey.pem;
#
#     location / {
#         root /usr/share/nginx/html;
#         index index.html;
#         try_files $uri $uri/ /index.html;
#     }
#
#     location /api/ {
#         proxy_pass http://backend:9080/;
#         proxy_set_header Host $host;
#         proxy_set_header X-Real-IP $remote_addr;
#         proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
#         proxy_set_header X-Forwarded-Proto $scheme;
#     }
#
#     location /auth/ {
#         proxy_pass http://backend:9080/auth/;
#         proxy_set_header Host $host;
#         proxy_set_header X-Real-IP $remote_addr;
#         proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
#         proxy_set_header X-Forwarded-Proto $scheme;
#     }
# }
```

### 8.2 배포 실행
```bash
cd ~/myauth

# 프로덕션 환경으로 실행
docker-compose -f docker-compose.prod.yml up -d --build

# 로그 확인
docker-compose -f docker-compose.prod.yml logs -f
```

### 8.3 컨테이너 상태 확인
```bash
# 실행 중인 컨테이너 확인
docker ps

# 각 컨테이너 로그 확인
docker logs prod-mysql
docker logs prod-backend
docker logs prod-nginx
```

---

## 9. 도메인 연결 (선택)

### 9.1 DNS 설정
도메인 등록 업체 (가비아, Route53 등)에서:
1. A 레코드 추가: `yourdomain.com` → EC2 공인 IP
2. A 레코드 추가: `www.yourdomain.com` → EC2 공인 IP

### 9.2 Nginx 설정 업데이트
```bash
vi ~/myauth/nginx/conf.d/default.conf

# server_name을 실제 도메인으로 변경
# server_name yourdomain.com www.yourdomain.com;
```

### 9.3 Nginx 재시작
```bash
docker-compose -f docker-compose.prod.yml restart nginx
```

---

## 10. SSL/HTTPS 설정 (선택)

### 10.1 Certbot 설치
```bash
sudo dnf install -y certbot python3-certbot-nginx
```

### 10.2 Let's Encrypt 인증서 발급
```bash
# Nginx 임시 중지
docker-compose -f docker-compose.prod.yml stop nginx

# 인증서 발급
sudo certbot certonly --standalone \
  -d yourdomain.com \
  -d www.yourdomain.com \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email

# 발급된 인증서 위치:
# /etc/letsencrypt/live/yourdomain.com/fullchain.pem
# /etc/letsencrypt/live/yourdomain.com/privkey.pem
```

### 10.3 Docker Compose에 SSL 마운트
```bash
vi ~/myauth/docker-compose.prod.yml

# nginx 서비스에 볼륨 추가:
# volumes:
#   - /etc/letsencrypt:/etc/nginx/ssl:ro
```

### 10.4 Nginx HTTPS 설정 활성화
```bash
vi ~/myauth/nginx/conf.d/default.conf

# HTTPS 서버 블록 주석 해제하고 HTTP→HTTPS 리다이렉트 활성화
```

### 10.5 재시작
```bash
docker-compose -f docker-compose.prod.yml up -d
```

### 10.6 자동 갱신 설정
```bash
# Cron job 추가 (매월 1일 인증서 갱신)
sudo crontab -e

# 다음 라인 추가:
0 0 1 * * certbot renew --quiet && docker-compose -f /home/ec2-user/myauth/docker-compose.prod.yml restart nginx
```

---

## 11. 문제 해결

### 11.1 컨테이너가 시작되지 않을 때
```bash
# 로그 확인
docker-compose -f docker-compose.prod.yml logs

# 특정 컨테이너 로그
docker logs prod-backend

# 컨테이너 재시작
docker-compose -f docker-compose.prod.yml restart
```

### 11.2 데이터베이스 연결 실패
```bash
# MySQL 컨테이너 접속
docker exec -it prod-mysql mysql -u root -p

# 데이터베이스 확인
SHOW DATABASES;
USE myauth;
SHOW TABLES;
```

### 11.3 환경 변수가 적용되지 않을 때
```bash
# .env 파일 확인
cat ~/myauth/.env

# 컨테이너 환경 변수 확인
docker exec prod-backend env | grep DB_URL

# 재빌드
docker-compose -f docker-compose.prod.yml down
docker-compose -f docker-compose.prod.yml up -d --build
```

### 11.4 포트가 이미 사용 중일 때
```bash
# 포트 사용 확인
sudo lsof -i :80
sudo lsof -i :443

# 프로세스 종료
sudo kill -9 <PID>
```

### 11.5 디스크 공간 부족
```bash
# 디스크 사용량 확인
df -h

# Docker 정리
docker system prune -a --volumes

# 로그 파일 정리
sudo journalctl --vacuum-time=3d
```

---

## 12. 유지보수

### 12.1 업데이트 배포
```bash
cd ~/myauth

# 최신 코드 가져오기
git pull

# React 빌드 (변경 시)
cd frontend && npm run build && cd ..

# 재배포
docker-compose -f docker-compose.prod.yml down
docker-compose -f docker-compose.prod.yml up -d --build
```

### 12.2 백업
```bash
# MySQL 백업
docker exec prod-mysql mysqldump -u root -p myauth > backup_$(date +%Y%m%d).sql

# 볼륨 백업
docker run --rm \
  -v prod-mysql-data:/data \
  -v $(pwd):/backup \
  alpine tar czf /backup/mysql-data-$(date +%Y%m%d).tar.gz /data
```

### 12.3 모니터링
```bash
# 컨테이너 리소스 사용량
docker stats

# 로그 실시간 확인
docker-compose -f docker-compose.prod.yml logs -f --tail=100
```

---

## 13. 완료!

축하합니다! AWS EC2에 프로젝트 배포가 완료되었습니다.

### 접속 확인
- HTTP: `http://your-ec2-ip` 또는 `http://yourdomain.com`
- HTTPS: `https://yourdomain.com` (SSL 설정 후)

### 다음 단계
1. CloudWatch로 모니터링 설정
2. Auto Scaling 구성 (트래픽 증가 시)
3. RDS로 MySQL 마이그레이션 (프로덕션 환경)
4. CloudFront CDN 설정 (성능 향상)
5. 백업 자동화

---

**문의사항이나 문제가 있으면 이슈를 등록해주세요!**
