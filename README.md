# Error Radar - 실시간 장애 감지 플랫폼

> Spring Boot, Kafka 및 Redis 기반 실시간 에러 패턴 감지, JWT 인증, Slack 알림 시스템
> 실무에서 반복되는 에러 패턴을 감지하고 즉시 알림을 발송합니다.

<br>

## 주요 기능

- **에러 로그 수집** - 애플리케이션에서 발생한 에러 로그 수집 API
- **실시간 패턴 감지** - Redis TTL 기반 시간 윈도우(30분) 내 에러 카운팅
- **자동 장애 알림** - 임계치(3회) 초과 시 Slack Webhook으로 즉시 알림 발송(3회로 수정해서 테스트 진행)
- **중복 알림 방지** - 알림 발송 후 Redis 카운트 초기화로 중복 알림 방지
- **장애 이력 관리** - MySQL에 에러 로그 영구 저장 및 조회
- **서비스별 조회** - 서비스명, 장애 감지 여부로 로그 필터링
- **JWT 인증/인가** - Access Token(1시간)과 Refresh Token(7일) 기반 인증
- **역할 기반 접근 제어** - ADMIN/USER 역할별 API 접근 권한 분리
- **실시간 모니터링** - Prometheus와 Grafana로 에러 수집, 알림, 로그인 메트릭 시각화

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
| Message Queue | Apache Kafka 4.1.2 | 비동기 로그 처리 파이프라인 |
| Security | Spring Security 7 + JWT | Stateless 인증, 역할 기반 접근 제어 |
| JWT | jjwt 0.12.6 | Access/Refresh Token 발급 및 검증 |
| 모니터링 | Prometheus + Grafana | 커스텀 메트릭 수집 및 시계열 시각화 |
| Metrics | Micrometer | 비즈니스 메트릭 코드 계측 |
| DB | MySQL 8.0 | 장애 이력 저장 |
| 알림 | Slack Webhook | 실무에서 많이 사용하는 알림 채널 |
| HTTP | RestClient | Slack API 호출 |
| Container | Docker | MySQL, Redis, Kafka 컨테이너 실행 |
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
→ 30분 내 3회 초과 시 Slack 알림 발송
→ 알림 발송 후 카운트 초기화 (중복 알림 방지)
→ 30분이 지나면 TTL 만료로 자동 초기화
```

### 서비스별 독립적 카운팅

```
error:count:order-service:NullPointerException   → 3회시 알림
error:count:payment-service:TimeoutException     → 3회 대기중
error:count:user-service:NullPointerException    → 12회 대기중

서비스명과 에러타입 조합으로 Key 생성
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
에러 로그 저장과 isAlerted 업데이트
→ 두 작업이 하나의 트랜잭션으로 처리
→ 부분 실패 없음
```

### JWT Stateless 인증과 Token Rotation(로그인 -> 토큰 발급 -> API 요청 -> 갱신 -> 로그아웃 흐름)

### Prometheus 커스텀 메트릭(errorradar.log.collected / alert.sent / login.success / login.fail)

<br>

## 시스템 아키텍처

```
[클라이언트 / 애플리케이션]
        │
        ▼ POST /api/logs/collect
[Spring Boot API]
        │
        ▼ Kafka topic(error-logs)에 메시지 발행
[202 Accepted 즉시 응답]

        ▼ (비동기)
[Kafka Consumer]
        │
        ├── MySQL에 에러 로그 저장
        │
        ├── Redis 에러 카운트 증가
        │   (TTL 30분 / Key: error:count:{service}:{errorType})
        │
        └── 임계치(3회) 초과
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
│   ├── KafkaConfig.java               # Kafka Producer/Consumer 설정
│   ├── KafkaTopicConfig.java          # Kafka 토픽 상수
│   ├── RedisConfig.java               # Redis 직렬화 설정
│   └── SwaggerConfig.java             # Swagger UI + Bearer Token 설정
├── auth/
│   ├── controller/
│   │   └── AuthController.java        # 회원가입/로그인/갱신/로그아웃 API
│   ├── service/
│   │   └── AuthService.java           # JWT 발급, Redis Refresh Token 관리
│   └── dto/
│       ├── SignupRequest.java          # 회원가입 요청 DTO
│       ├── LoginRequest.java           # 로그인 요청 DTO
│       ├── RefreshRequest.java         # 토큰 갱신 요청 DTO
│       └── AuthResponse.java           # 인증 응답 DTO (토큰 + 사용자 정보)
├── user/
│   ├── entity/
│   │   ├── User.java                  # 사용자 엔티티 (JPA)
│   │   └── Role.java                  # 역할 Enum (ROLE_ADMIN, ROLE_USER)
│   └── repository/
│       └── UserRepository.java        # 사용자 JpaRepository
├── log/
│   ├── producer/
│   │   └── LogProducerService.java    # Kafka 메시지 발행
│   ├── consumer/
│   │   └── LogConsumerService.java    # Kafka 메시지 수신 및 처리
│   ├── entity/
│   │   └── ErrorLog.java             # 에러 로그 엔티티 (JPA)
│   ├── repository/
│   │   └── ErrorLogRepository.java    # 에러 로그 JpaRepository
│   ├── service/
│   │   ├── LogService.java            # 로그 수집 인터페이스
│   │   └── impl/
│   │       └── LogServiceImpl.java    # 로그 수집 구현체
│   ├── controller/
│   │   └── LogController.java         # 로그 수집 REST API
│   └── dto/
│       ├── LogEvent.java              # Kafka 메시지 DTO
│       ├── LogRequest.java            # 로그 수집 요청 DTO
│       └── LogResponse.java           # 로그 수집 응답 DTO
├── pattern/
│   └── service/
│       └── PatternDetectService.java  # Redis 에러 패턴 감지
├── alert/
│   └── service/
│       └── AlertService.java          # Slack 장애 알림 발송
└── global/
    ├── security/
    │   ├── SecurityConfig.java        # Spring Security 필터 체인 설정
    │   ├── JwtUtil.java               # JWT 생성/검증 유틸
    │   ├── JwtAuthenticationFilter.java  # JWT 인증 필터 (OncePerRequestFilter)
    │   └── UserDetailsServiceImpl.java   # DB 기반 사용자 로드
    ├── metrics/
    │   └── ErrorRadarMetrics.java     # Micrometer 커스텀 메트릭 (Counter)
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
    webhook-url: https://hooks.slack.com/services/.../.../...
```

### 3. Docker 컨테이너 실행
```
docker-compose up -d
```

### 4. 애플리케이션 실행
```
ErrorRadarApplication 실행
```

### 5. Swagger UI 접속
```
http://localhost:8080/swagger-ui.html
```

<br>

## API 명세

| Method | URL | 설명 |
|--------|-----|------|
| POST | /api/logs/collect | 에러 로그 수집 (Kafka 비동기 처리) |
| GET | /api/logs/getLogs | 전체 에러 로그 조회 |
| GET | /api/logs/service/{serviceName} | 서비스별 에러 로그 조회 |
| GET | /api/logs/alerted | 장애 감지된 로그 조회 |
| POST | /api/auth/signup | 회원가입 |                       
| POST | /api/auth/login  | 로그인, 토큰 발급 |              
| POST | /api/auth/refresh | 토큰 재발급 |
| POST | /api/auth/logout  | 로그아웃 |

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
• 발생 횟수: 3회 / 30분 이내
• 에러 메시지: null pointer at OrderService.java:52

즉시 확인이 필요합니다!
```

<br>

## 임계치 설정

```
alert:
  threshold:
    count: 3           # 에러 발생 횟수 임계치
    window-minutes: 30 # 시간 윈도우 (분)
  slack:
    webhook-url: ""    # Slack Webhook URL
```

<br>

## 개선 과제

- **Spring AOP 적용** - 예외 발생 시 자동으로 에러 로그 수집 (현재는 API 직접 호출)
- **에러 통계 대시보드** - 서비스별 에러 발생 추이 시각화
- **알림 채널 확장** - 이메일, 카카오톡 등 다양한 알림 채널 지원
- **입력값 검증 강화** - @Valid와 @Email, @Size 기반 요청 파라미터 검증
- **환경변수 분리** - JWT Secret 등 민감 정보를 환경변수로 외부화
    
<br>

## 트러블슈팅

### 1. Windows 한글 경로로 인한 Gradle 테스트 실패

**증상**
```
Error occurred during initialization of VM
java.lang.ClassNotFoundException: worker.org.gradle.process.internal.worker.GradleWorkerMain
```

**원인**

Gradle은 테스트를 별도 JVM 프로세스로 실행
이때 JVM 인수가 너무 길어지면 argfile(`@파일경로`) 방식으로 인수를 파일에 쓰고 Java에 넘김

```
# Gradle이 내부적으로 이런 방식으로 worker를 실행
java @C:\Users\이희재\.gradle\.tmp\gradle-worker-classpath123.txt worker.GradleWorkerMain
```

이 argfile이 `GRADLE_USER_HOME/.tmp/` 하위에 UTF-8로 작성되는데,
기본 경로(`C:\Users\이희재\.gradle`)에 한글이 포함되면
Windows 한국어로 파일을 읽는 Java가 경로를 오인식해 JAR를 찾지 못함

에러 메시지만 보면 Gradle 내부 클래스 문제처럼 보여서 원인을 파악하기가 매우 어려웠음

**해결**

`gradle.properties`에 한글이 없는 경로로 지정
```properties
org.gradle.user.home=C:/gradle-home
```

단, 기존 daemon이 살아있으면 위 설정 무시
반드시 환경변수 설정 후 daemon을 재시작해야함

---

### 2. GitHub Actions(Linux)에서 Windows 전용 tmpdir 경로 오류

**증상**
```
WARNING: java.io.tmpdir directory does not exist: C:/Temp/kafka-test
org.apache.kafka.common.KafkaException: Failed to create local log directory
MockitoInitializationException: Could not self-attach to current VM
```

**원인**

- EmbeddedKafka는 Windows에서 기본 `C:\Temp\`에 임시 파일을 쓰는데,
일반적으로 이 경로가 없어서 테스트가 실패
- 이를 해결하기 위해 `build.gradle`에 아래 설정 추가

```groovy
// 문제가 된 코드
tasks.named('test') {
    useJUnitPlatform()
    systemProperty 'java.io.tmpdir', 'C:/Temp/kafka-test'  // Windows 전용 경로
}
```

로컬에서는 잘 동작했지만, GitHub Actions에 푸시하니
`C:/Temp/kafka-test` 경로가 Linux에 존재하지 않아 EmbeddedKafka와 Mockito가 함께 실패

**해결**

OS를 감지해 Windows에서만 해당 tmpdir을 적용
```groovy
tasks.named('test') {
    useJUnitPlatform()
    if (System.getProperty('os.name').toLowerCase().contains('windows')) {
        systemProperty 'java.io.tmpdir', 'C:/Temp/kafka-test'
    }
}
```

---

### 3. Kafka Consumer 파티션 할당 지연으로 인한 통합 테스트 타임아웃

**증상**
```
org.awaitility.core.ConditionTimeoutException:
  Condition with ... was not fulfilled within 10 seconds.
```

**원인**

Kafka Consumer는 비동기로 동작하기 때문에 아래와 같이 테스트하면 항상 실패

```
// 잘못된 방법 - Consumer가 아직 처리 안 했으니 DB에 데이터가 없음
logProducerService.sendLog(event);  // Kafka에 발행
assertThat(errorLogRepository.count()).isEqualTo(1);  // 즉시 확인 → 실패
```

- 그래서 Awaitility로 "DB에 데이터가 생길 때까지 기다려" 라고 지정하는데,
EmbeddedKafka 환경에서 Consumer가 최초 파티션을 할당받기까지 `NOT_COORDINATOR` 재시도로
약 9초 소요
- 타임아웃을 10초로 설정하면 DB 저장 시점이 타임아웃 직후가 되어 간헐적으로 실패

```
// 타임아웃이 너무 짧아서 실패하는 코드
Awaitility.await()
    .atMost(Duration.ofSeconds(10))  // Consumer 파티션 할당에만 ~9초 소요
    .until(() -> errorLogRepository.count() == 1);
```

**해결**

Awaitility 타임아웃을 30초로 늘리고 폴링 간격 명시:
```
Awaitility.await()
    .atMost(Duration.ofSeconds(30))      // 최대 30초까지 대기
    .pollInterval(Duration.ofSeconds(1)) // 1초마다 조건 확인
    .until(() -> errorLogRepository.count() == 1);
```

### 4. Grafana N/A - /actuator/prometheus가 Security에 막혀서 수집 실패 -> SecurityConfig에서 permitAll() 추가로 해결                  
                                                                                                                                                                                                                                   
### 5. Spring Boot 4 ObjectMapper 자동 주입 실패 (Jackson 3 패키지 변경) -> SecurityConfig에서 ObjectMapper 제거하고 JSON 문자열 직접 작성으로 해결 
