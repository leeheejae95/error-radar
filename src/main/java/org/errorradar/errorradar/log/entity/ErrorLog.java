package org.errorradar.errorradar.log.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "error_logs")
public class ErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 서비스명
    @Column(name = "service_name", nullable = false)
    private String serviceName;

    // 에러 타입
    @Column(name = "error_type", nullable = false)
    private String errorType;

    // 에러 메시지
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // 발생 환경 (개발서버, 운영서버)
    @Column(name = "environment")
    private String environment;

    // 발생여부
    @Column(name = "is_alerted")
    private boolean isAlerted;

    // 발생시각
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Builder
    public ErrorLog(String serviceName, String errorType, String errorMessage, String environment) {
        this.serviceName = serviceName;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.environment = environment;
        this.isAlerted = false;
        this.occurredAt = LocalDateTime.now();
    }

    public void markAsAlerted() {
        this.isAlerted = true;
    }

}
