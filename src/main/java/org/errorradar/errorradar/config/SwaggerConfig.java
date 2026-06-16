package org.errorradar.errorradar.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Error Radar API")
                        .description("실시간 에러 팬턴 감지 및 Slack 알림 시스템")
                        .version("v1.0.0")
                );

    }
}
