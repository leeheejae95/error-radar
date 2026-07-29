package org.errorradar.errorradar.log.service;

import org.errorradar.errorradar.log.dto.LogResponse;

import java.util.List;

public interface LogService {

    List<LogResponse> getLogs();

    List<LogResponse> getLogsByService(String serviceName);

    List<LogResponse> getAlertedLogs();
}
