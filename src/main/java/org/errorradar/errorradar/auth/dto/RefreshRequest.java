package org.errorradar.errorradar.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RefreshRequest {
    private String refreshToken;
}
