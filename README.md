## 📁 프로젝트 구조

```
src/main/java/org/errorradar/errorradar/
├── config/
│   ├── RedisConfig.java          # Redis 직렬화 설정
│   └── SwaggerConfig.java        # Swagger UI 설정
├── log/
│   ├── entity/
│   │   └── ErrorLog.java         # 에러 로그 엔티티 (JPA)
│   ├── repository/
│   │   └── ErrorLogRepository.java  # 에러 로그 JpaRepository
│   ├── service/
│   │   ├── LogService.java       # 로그 수집 인터페이스
│   │   └── impl/
│   │       └── LogServiceImpl.java  # 로그 수집 구현체
│   ├── controller/
│   │   └── LogController.java    # 로그 수집 REST API
│   └── dto/
│       ├── LogRequest.java       # 로그 수집 요청 DTO
│       └── LogResponse.java      # 로그 수집 응답 DTO
├── pattern/
│   └── service/
│       └── PatternDetectService.java  # Redis 에러 패턴 감지
├── alert/
│   └── service/
│       └── AlertService.java     # Slack 장애 알림 발송
└── global/
    ├── exception/
    │   ├── ErrorCode.java        # 에러 코드 Enum
    │   └── CustomException.java  # 커스텀 예외
    ├── handler/
    │   └── GlobalExceptionHandler.java  # 전역 예외 처리
    └── response/
        └── ApiResponse.java      # 공통 응답 포맷
```
