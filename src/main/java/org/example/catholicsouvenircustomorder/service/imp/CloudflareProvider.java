package org.example.catholicsouvenircustomorder.service.imp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.service.SupabaseStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

@Service
@Slf4j
public class CloudflareProvider {

    @Value("${cloudflare.account-id:}")
    private String accountId;

    @Value("${cloudflare.api-token:}")
    private String apiToken;

    @Value("${cloudflare.model:@cf/black-forest-labs/flux-1-schnell}")
    private String model;

    private final RestTemplate restTemplate;
    private final SupabaseStorageService storageService;

    public CloudflareProvider(SupabaseStorageService storageService) {
        this.storageService = storageService;
        this.restTemplate = new RestTemplate();
        this.restTemplate.setRequestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
            setConnectTimeout(30000);
            setReadTimeout(120000);
        }});
    }

    public String generateImage(String prompt) {
        // Check if Cloudflare credentials are configured
        if (apiToken == null || apiToken.isEmpty()) {
            log.error("❌ Cloudflare API token not configured");
            return null;
        }
        
        if (accountId == null || accountId.isEmpty()) {
            log.error("❌ Cloudflare account ID not configured");
            return null;
        }
        
        log.info("🔑 Using Cloudflare Account: {}", accountId);
        log.info("🔑 API Token length: {}", apiToken.length());
        
        try {
            String url = "https://api.cloudflare.com/client/v4/accounts/"
                    + accountId + "/ai/run/" + model;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of("prompt", prompt);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            log.info("🎨 Generating image with Cloudflare AI: {}", model);
            log.info("📝 Prompt: {}", prompt);
            log.info("🌐 API URL: {}", url);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    byte[].class
            );

            log.info("📥 Response status: {}", response.getStatusCode());
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                byte[] responseBody = response.getBody();
                log.info("📦 Response body size: {} bytes", responseBody.length);
                
                // Check if response is JSON (contains base64 image)
                String responseStr = new String(responseBody);
                if (responseStr.trim().startsWith("{")) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(responseStr);
                    
                    log.info("📄 JSON Response: {}", responseStr.substring(0, Math.min(200, responseStr.length())));
                    
                    // Check for error
                    if (root.has("errors") && root.get("errors").isArray() && root.get("errors").size() > 0) {
                        log.error("❌ Cloudflare API error: {}", root.get("errors"));
                        return null;
                    }
                    
                    // Extract base64 image from result
                    String base64 = root.path("result").path("image").asText();
                    if (base64 == null || base64.isEmpty()) {
                        log.error("❌ No image in response. Full response: {}", responseStr);
                        return null;
                    }
                    
                    log.info("✅ Got base64 image, length: {}", base64.length());
                    byte[] imageBytes = Base64.getDecoder().decode(base64);
                    return uploadOrBase64(imageBytes);
                } else {
                    // Raw image bytes
                    log.info("📷 Got raw image bytes");
                    return uploadOrBase64(responseBody);
                }
            }

            log.error("❌ Cloudflare API returned status: {}", response.getStatusCode());
            return null;

        } catch (Exception e) {
            log.error("❌ CloudflareProvider error: {}", e.getMessage(), e);
            return null;
        }
    }

    private String uploadOrBase64(byte[] imageBytes) {
        String fileName = "cf_" + System.currentTimeMillis();
        String url = storageService.uploadImage(imageBytes, fileName);
        
        if (url != null) {
            log.info("✅ Image uploaded to Supabase: {}", url);
            return url;
        }
        
        log.warn("⚠️ Failed to upload to Supabase, returning base64");
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
    }
}
