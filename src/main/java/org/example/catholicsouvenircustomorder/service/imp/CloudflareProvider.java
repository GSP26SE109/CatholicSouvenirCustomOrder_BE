package org.example.catholicsouvenircustomorder.service.imp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.service.AIImageProvider;
import org.example.catholicsouvenircustomorder.service.SupabaseStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

@Service
@Slf4j
public class CloudflareProvider implements AIImageProvider {

    @Value("${cloudflare.account-id}")
    private String accountId;

    @Value("${cloudflare.api-token}")
    private String apiToken;

    @Value("${cloudflare.model:@cf/black-forest-labs/flux-1-schnell}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final SupabaseStorageService storageService;

    public CloudflareProvider(SupabaseStorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public String generateImage(String prompt) {
        try {
            String url = "https://api.cloudflare.com/client/v4/accounts/"
                    + accountId + "/ai/run/" + model;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "prompt", prompt
            );

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(body, headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            String.class
                    );

            String responseBody = response.getBody();

            if (responseBody == null) return null;

// Case 1: JSON response
            if (responseBody.startsWith("{")) {
                ObjectMapper mapper = new ObjectMapper();

                JsonNode root = mapper.readTree(responseBody);
                String base64 = root.path("result").path("image").asText();

                if (base64 == null || base64.isEmpty()) return null;

                byte[] imageBytes = Base64.getDecoder().decode(base64);

                return uploadOrBase64(imageBytes);
            }

// Case 2: raw image (rare but safe fallback)
            byte[] imageBytes = responseBody.getBytes();
            return uploadOrBase64(imageBytes);

        } catch (Exception e) {
            log.error("CloudflareProvider error: {}", e.getMessage());
        }

        return null;
    }
    private String uploadOrBase64(byte[] imageBytes) {
        String fileName = "cf_" + System.currentTimeMillis();

        String url = storageService.uploadImage(imageBytes, fileName);

        if (url != null) return url;

        return "data:image/png;base64," +
                Base64.getEncoder().encodeToString(imageBytes);
    }
}
