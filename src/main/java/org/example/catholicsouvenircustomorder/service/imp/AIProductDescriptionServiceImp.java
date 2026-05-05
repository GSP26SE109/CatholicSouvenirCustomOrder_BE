package org.example.catholicsouvenircustomorder.service.imp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.response.Product.GenerateDescriptionResponse;
import org.example.catholicsouvenircustomorder.service.AIProductDescriptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class AIProductDescriptionServiceImp implements AIProductDescriptionService {

    @Value("${huggingface.api.key:}")
    private String huggingfaceApiKey;

    @Value("${huggingface.text.model:meta-llama/Llama-3.1-8B-Instruct:novita}")
    private String huggingfaceTextModel;

    @Value("${huggingface.api.url:https://router.huggingface.co/v1/chat/completions}")
    private String huggingfaceApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String generateDescription(String productName, String category, String tags, String existingDescription) {
        GenerateDescriptionResponse response = generateDescriptionDetailed(productName, category, tags, existingDescription);
        return response.getDescription();
    }

    @Override
    public GenerateDescriptionResponse generateDescriptionDetailed(String productName, String category, String tags, String existingDescription) {
        try {
            log.info("Generating AI description for product: {}, category: {}", productName, category);

            // Try AI first if API key is configured
            if (huggingfaceApiKey != null && !huggingfaceApiKey.isEmpty()) {
                try {
                    String prompt = buildDescriptionPrompt(productName, category, tags, existingDescription);
                    String aiDescription = callHuggingFace(prompt);
                    
                    if (aiDescription != null && !aiDescription.trim().isEmpty()) {
                        log.info("AI description generated successfully");
                        return GenerateDescriptionResponse.builder()
                                .description(cleanDescription(aiDescription))
                                .aiGenerated(true)
                                .message("Mô tả được tạo bởi AI")
                                .build();
                    }
                } catch (Exception e) {
                    log.warn("AI service unavailable, using fallback: {}", e.getMessage());
                }
            }
            
            // Fallback to template-based description
            String fallbackDesc = generateFallbackDescription(productName, category, tags, existingDescription);
            return GenerateDescriptionResponse.builder()
                    .description(fallbackDesc)
                    .aiGenerated(false)
                    .message("Mô tả được tạo từ template (AI không khả dụng)")
                    .build();

        } catch (Exception e) {
            log.error("Error generating description: {}", e.getMessage(), e);
            String fallbackDesc = generateFallbackDescription(productName, category, tags, existingDescription);
            return GenerateDescriptionResponse.builder()
                    .description(fallbackDesc)
                    .aiGenerated(false)
                    .message("Mô tả được tạo từ template (có lỗi xảy ra)")
                    .build();
        }
    }

    private String buildDescriptionPrompt(String productName, String category, String tags, String existingDescription) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are a Catholic product description writer. Create a detailed, faith-based product description.\n\n");
        prompt.append("Product Name: ").append(productName).append("\n");
        
        if (category != null && !category.isEmpty()) {
            prompt.append("Category: ").append(category).append("\n");
        }
        
        if (tags != null && !tags.isEmpty()) {
            prompt.append("Tags: ").append(tags).append("\n");
        }
        
        if (existingDescription != null && !existingDescription.trim().isEmpty()) {
            prompt.append("Existing Description: ").append(existingDescription).append("\n\n");
            prompt.append("Please enhance and expand this description.\n");
        }
        
        prompt.append("\nRequirements:\n");
        prompt.append("- Write in Vietnamese language\n");
        prompt.append("- Include spiritual and faith-based elements\n");
        prompt.append("- Mention how this product can deepen Catholic faith\n");
        prompt.append("- Keep it between 100-200 words\n");
        prompt.append("- Be warm, respectful, and inspiring\n");
        prompt.append("- Focus on the religious significance and practical use\n");
        prompt.append("- Do not include any markdown formatting or special characters\n\n");
        prompt.append("Write the description now:");
        
        return prompt.toString();
    }

    private String callHuggingFace(String prompt) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(huggingfaceApiKey);

        List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", "You are a Catholic product description writer specializing in religious souvenirs and items. Write in Vietnamese."),
            Map.of("role", "user", "content", prompt)
        );

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", huggingfaceTextModel);
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 400);
        requestBody.put("temperature", 0.7);
        requestBody.put("stream", false);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    huggingfaceApiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String content = root.path("choices").get(0).path("message").path("content").asText();
                return content;
            }

            throw new Exception("Hugging Face API returned non-OK status: " + response.getStatusCode());
        } catch (Exception e) {
            log.error("Hugging Face API call failed: {}", e.getMessage());
            throw e;
        }
    }

    private String cleanDescription(String description) {
        if (description == null) return "";
        
        // Remove markdown formatting
        description = description.replaceAll("\\*\\*", "");
        description = description.replaceAll("\\*", "");
        description = description.replaceAll("##", "");
        description = description.replaceAll("#", "");
        
        // Remove extra whitespace
        description = description.trim();
        description = description.replaceAll("\\n{3,}", "\n\n");
        
        return description;
    }

    private String generateFallbackDescription(String productName, String category, String tags, String existingDescription) {
        // If existing description is provided, return it
        if (existingDescription != null && !existingDescription.trim().isEmpty()) {
            return existingDescription;
        }
        
        // Generate template-based description
        StringBuilder description = new StringBuilder();
        
        description.append(productName).append(" là một sản phẩm lưu niệm Công Giáo ");
        
        if (category != null && !category.isEmpty()) {
            description.append("thuộc danh mục ").append(category).append(", ");
        }
        
        description.append("được chế tác tỉ mỉ với tâm huyết và lòng sùng kính. ");
        description.append("Sản phẩm này không chỉ là một vật dụng trang trí mà còn là ");
        description.append("một công cụ giúp bạn gần gũi hơn với Chúa trong cuộc sống hàng ngày. ");
        
        if (tags != null && !tags.isEmpty()) {
            description.append("Phù hợp cho ").append(tags.toLowerCase()).append(". ");
        }
        
        description.append("Hãy để sản phẩm này đồng hành cùng bạn trên hành trình đức tin.");
        
        return description.toString();
    }
}
