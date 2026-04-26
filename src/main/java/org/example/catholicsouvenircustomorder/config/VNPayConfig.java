package org.example.catholicsouvenircustomorder.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "vnpay")
public class VNPayConfig {
    private String tmnCode;
    private String hashSecret;
    private String url;
    private String returnUrl;
    private String stageReturnUrl;  // Separate return URL for stage payments
    private String ipnUrl;
    private String version;
    private String command;
    private String orderType;
    private String apiUrl;  // API URL for refund operations
}
