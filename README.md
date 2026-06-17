# Error Radar - 실시간 장애 감지 플랫폼

> Spring Boot + Redis 기반 실시간 에러 패턴 감지 및 Slack 알림 시스템
> 실무에서 반복되는 에러 패턴을 감지하고 즉시 알림을 발송합니다.

<br>

## 주요 기능

- **에러 로그 수집** - 애플리케이션에서 발생한 에러 로그 수집 API
- **실시간 패턴 감지** - Redis TTL 기반 시간 윈도우(30분) 내 에러 카운팅
- **자동 장애 알림** - 임계치(50회) 초과 시 Slack Webhook으로 즉시 알림 발송(3회로 수정해서 테스트 진행)
- **중복 알림 방지** - 알림 발송 후 Redis 카운트 초기화로 중복 알림 방지
- **장애 이력 관리** - MySQL에 에러 로그 영구 저장 및 조회
- **서비스별 조회** - 서비스명, 장애 감지 여부로 로그 필터링

<br>

## 개발 배경

Jennifer APM으로 장애를 분석하면서 동일한 에러 패턴이 반복적으로 발생하는 것을 경험했습니다.
매번 수동으로 확인하는 방식의 비효율을 해결하기 위해,
에러 패턴을 자동으로 감지하고 Slack으로 즉시 알림을 주는 시스템을 직접 구현했습니다.

실제 성과: 월 장애 100건에서 30건미만으로 감소시킨 경험을 바탕으로 설계했습니다.

<br>

## 기술 스택

| 분류 | 기술 | 선택 이유 |
|------|------|----------|
| Language | Java 21 |  |
| Framework | Spring Boot 4.0.6 |  |
| ORM | Spring Data JPA | 에러 로그 저장 |
| Cache | Redis 7.2 | TTL 기반 시간 윈도우 카운팅 |
| DB | MySQL 8.0 | 장애 이력 저장 |
| 알림 | Slack Webhook | 실무에서 많이 사용하는 알림 채널 |
| HTTP | RestClient | Slack API 호출 |
| Container | Docker | MySQL, Redis 컨테이너 실행 |
| 문서화 | Swagger | API 테스트 및 문서화 |

<br>

## 핵심 설계 포인트

### Redis TTL 기반 시간 윈도우

```
Redis Key: error:count:{serviceName}:{errorType}
TTL: 30분 (window-minutes)

NullPointerException 발생
→ error:count:order-service:NullPointerException 카운트 증가
→ 처음 생성 시 TTL 30분 설정
→ 30분 내 50회 초과 시 Slack 알림 발송
→ 알림 발송 후 카운트 초기화 (중복 알림 방지)
→ 30분이 지나면 TTL 만료로 자동 초기화
```

### 서비스별 독립적 카운팅

```
error:count:order-service:NullPointerException   → 50회 → 알림!
error:count:payment-service:TimeoutException      → 3회  → 대기중
error:count:user-service:NullPointerException     → 12회 → 대기중

서비스명 + 에러타입 조합으로 Key 생성
→ 각 서비스/에러 타입별 독립적 감지
```

### 인터페이스 기반 설계

```
LogService (인터페이스)
    ↓
LogServiceImpl (구현체)

→ 비즈니스 로직 변경 없이 구현체만 교체 가능
→ MSA 전환 시 서비스 분리 용이
```

### @Transactional 원자성 보장

```
에러 로그 저장 + isAlerted 업데이트
→ 두 작업이 하나의 트랜잭션으로 처리
→ 부분 실패 없음
```

<br>

## 시스템 아키텍처

```
[클라이언트 / 애플리케이션]
        │
        ▼ POST /api/logs/collect
[Spring Boot API]
        │
        ├── MySQL에 에러 로그 저장
        │
        ├── Redis 에러 카운트 증가
        │   (TTL 30분 / Key: error:count:{service}:{errorType})
        │
        └── 임계치(50회) 초과
                │ YES
                ▼
        [Slack Webhook 알림 발송]
                │
        isAlerted = true 업데이트
                │
        Redis 카운트 초기화 (중복 방지)
```

<br>

## 프로젝트 구조

```
src/main/java/org/errorradar/errorradar/
├── config/
│   ├── RedisConfig.java               # Redis 직렬화 설정
│   └── SwaggerConfig.java             # Swagger UI 설정
├── log/
│   ├── entity/						   # 에러 로그 엔티티 (JPA)
│   │   └── ErrorLog.java              # 에러 로그 엔티티 (JPA)
│   ├── repository/
│   │   └── ErrorLogRepository.java    # 에러 로그 JpaRepository
│   ├── service/
│   │   ├── LogService.java            # 로그 수집 인터페이스
│   │   └── impl/
│   │       └── LogServiceImpl.java    # 로그 수집 구현체
│   ├── controller/
│   │   └── LogController.java         # 로그 수집 REST API
│   └── dto/
│       ├── LogRequest.java            # 로그 수집 요청 DTO
│       └── LogResponse.java           # 로그 수집 응답 DTO
├── pattern/
│   └── service/
│       └── PatternDetectService.java  # Redis 에러 패턴 감지
├── alert/
│   └── service/
│       └── AlertService.java          # Slack 장애 알림 발송
└── global/
    ├── errorcode/
    │   └── ErrorCode.java             # 에러 코드 Enum
    ├── exception/
    │   └── CustomException.java       # 커스텀 예외
    ├── handler/
    │   └── GlobalExceptionHandler.java  # 전역 예외 처리
    └── response/
        └── ApiResponse.java           # 공통 응답 포맷
```

<br>

## 로컬 실행 방법

### 사전 요구사항
- Java 21
- Docker Desktop

### 1. 레포지토리 클론
```
git clone https://github.com/leeheejae95/error-radar.git
cd error-radar
```

### 2. Slack Webhook URL 설정
```
# src/main/resources/application.yml
alert:
  slack:
    webhook-url: https://hooks.slack.com/services/xxx/yyy/zzz
```

### 3. Docker 컨테이너 실행
```
docker-compose up -d
```

### 4. 애플리케이션 실행
```
./gradlew bootRun
```

### 5. Swagger UI 접속
```
http://localhost:8080/swagger-ui.html
```

<br>

## API 명세

| Method | URL | 설명 |
|--------|-----|------|
| POST | /api/logs/collect | 에러 로그 수집 (패턴 감지 + Slack 알림) |
| GET | /api/logs/getLogs | 전체 에러 로그 조회 |
| GET | /api/logs/service/{serviceName} | 서비스별 에러 로그 조회 |
| GET | /api/logs/alerted | 장애 감지된 로그 조회 |

### 요청 예시
```
{
  "serviceName": "order-service",
  "errorType": "NullPointerException",
  "errorMessage": "null pointer at OrderService.java:52",
  "environment": "prod"
}
```

### Slack 알림 예시
```
  장애 경고 발생!

• 서비스: order-service
• 에러 타입: NullPointerException
• 발생 횟수: 50회 / 30분 이내
• 에러 메시지: null pointer at OrderService.java:52

즉시 확인이 필요합니다!
```

<br>

## 임계치 설정

```
alert:
  threshold:
    count: 50          # 에러 발생 횟수 임계치
    window-minutes: 30 # 시간 윈도우 (분)
  slack:
    webhook-url: ""    # Slack Webhook URL
```

<br>

## 개선 과제

- **Spring AOP 적용** - 예외 발생 시 자동으로 에러 로그 수집 (현재는 API 직접 호출)
- **에러 통계 대시보드** - 서비스별 에러 발생 추이 시각화
- **알림 채널 확장** - 이메일, 카카오톡 등 다양한 알림 채널 지원
- **GitHub Actions CI/CD** - 자동 빌드 및 테스트
- **MSA 전환** - 도메인 패키지 기반으로 서비스 분리 가능한 구조
