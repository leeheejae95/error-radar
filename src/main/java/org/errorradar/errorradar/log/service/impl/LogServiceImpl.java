package org.errorradar.errorradar.log.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.errorradar.errorradar.log.dto.LogResponse;
import org.errorradar.errorradar.log.repository.ErrorLogRepository;
import org.errorradar.errorradar.log.service.LogService;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private final ErrorLogRepository errorLogRepository;

    @Override
    public List<LogResponse> getLogs() {
        return errorLogRepository.findAll()
                .stream()
                .map(LogResponse::from)
                .toList();
    }

    @Override
    public List<LogResponse> getLogsByService(String serviceName) {
        return errorLogRepository.findByServiceNameOrderByOccurredAtDesc(serviceName)
                .stream()
                .map(LogResponse::from)
                .toList();
    }

    @Override
    public List<LogResponse> getAlertedLogs() {
        return errorLogRepository.findByIsAlertedTrue()
                .stream()
                .map(LogResponse::from)
                .toList();
    }
}
