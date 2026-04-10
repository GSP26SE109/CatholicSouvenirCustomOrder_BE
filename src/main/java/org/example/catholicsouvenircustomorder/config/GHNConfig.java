package org.example.catholicsouvenircustomorder.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ghn")
@Data
public class GHNConfig {
    private String apiUrl;
    private String token;
    private Integer shopId;
    private Integer fromDistrictId;
    private String fromWardCode;
}
