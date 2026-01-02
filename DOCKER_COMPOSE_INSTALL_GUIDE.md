# Amazon Linux 2023에 Docker + Docker Compose 설치 (2025년 최신)

📌 중요한 변경사항  

Amazon Linux 2023에서는 Docker Compose v2를 플러그인으로 설치해야 합니다.  
- ❌ 이전 방식: docker-compose (하이픈 있음, 독립 실행 파일)
- ✅ 새로운 방식: docker compose (하이픈 없음, Docker 플러그인)

## 설치 순서
아래 명령어들을 순서대로 실행.  

1️⃣ 시스템 업데이트  
- 시스템 패키지 업데이트
```commandline
sudo dnf update -y
```

2️⃣ Docker 설치  
- Amazon의 기본 저장소에서 Docker 설치
```bash
sudo dnf install docker -y
```

3️⃣ Docker 서비스 시작 및 활성화
```commandline
# Docker 서비스 시작
sudo systemctl start docker

# 부팅 시 자동 시작 설정
sudo systemctl enable docker

# Docker 상태 확인
sudo systemctl status docker
# 출력에 "active (running)" 표시되면 정상
```

4️⃣ 현재 사용자를 docker 그룹에 추가
- 현재 사용자를 docker 그룹에 추가 (sudo 없이 docker 명령 사용 가능)
```commandline
sudo usermod -aG docker $USER

# 그룹 변경사항 즉시 적용
newgrp docker
```

5️⃣ Docker Compose 플러그인 설치
# Docker 플러그인 디렉토리 생성
```commandline
sudo mkdir -p /usr/libexec/docker/cli-plugins

# Docker Compose 최신 버전 다운로드
sudo curl -SL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-$(uname -m)" \
-o /usr/libexec/docker/cli-plugins/docker-compose

# 실행 권한 부여
sudo chmod +x /usr/libexec/docker/cli-plugins/docker-compose
```


6️⃣ Docker 재시작
```commandline
sudo systemctl restart docker
```

7️⃣ 설치 확인
- Docker 버전 확인
```commandline
docker --version
```
- 출력 예: Docker version 25.0.x

- Docker Compose 버전 확인 (하이픈 없이!)
```commandline
docker compose version
```
출력 예: Docker Compose version v2.24.x