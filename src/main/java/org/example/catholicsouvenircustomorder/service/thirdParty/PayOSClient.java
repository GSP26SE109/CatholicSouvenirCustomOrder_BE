package org.example.catholicsouvenircustomorder.service.thirdParty;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.config.PayOSConfig;
import org.example.catholicsouvenircustomorder.model.Payment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PayOSClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final PayOSConfig config;

    public String createPaymentLink(Payment payment) {

        String url = "https://api.payos.vn/v2/payment-requests";

        Map<String, Object> body = new HashMap<>();
        body.put("orderCode", payment.getPaymentId());
        body.put("amount", payment.getAmount());
        body.put("description", buildDescription(payment));
        body.put("returnUrl", config.getReturnUrl());
        body.put("cancelUrl", config.getCancelUrl());

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-client-id", config.getClientId());
        headers.set("x-api-key", config.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(url, request, Map.class);

        Map data = (Map) response.getBody().get("data");

        return data.get("checkoutUrl").toString();
    }

    private String buildDescription(Payment payment) {
        if (payment.getStage() != null)
            return "Stage payment: " + payment.getStage().getName();
        return "Full order payment: " + payment.getOrder().getOrderId();
    }
}
