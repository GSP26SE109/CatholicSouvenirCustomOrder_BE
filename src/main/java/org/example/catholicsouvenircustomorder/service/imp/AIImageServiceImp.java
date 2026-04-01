package org.example.catholicsouvenircustomorder.service.imp;

import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.request.AIPromptRequest;
import org.example.catholicsouvenircustomorder.dto.response.AIImageResponse;
import org.example.catholicsouvenircustomorder.model.ProductTemplate;
import org.example.catholicsouvenircustomorder.model.TemplateCustomZone;
import org.example.catholicsouvenircustomorder.service.AIImageService;
import org.example.catholicsouvenircustomorder.service.SupabaseStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class AIImageServiceImp implements AIImageService {

    @Value("${ai.image.provider:huggingface}")
    private String imageProvider;

    @Value("${huggingface.api.key:}")
    private String huggingfaceApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final SupabaseStorageService supabaseStorageService;

    public AIImageServiceImp(SupabaseStorageService supabaseStorageService) {
        this.supabaseStorageService = supabaseStorageService;
    }

    @Override
    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2),
        retryFor = {Exception.class}
    )
    public String generateImage(String prompt) {
        try {
            String enhancedPrompt = enhancePromptForReligiousArt(prompt);
            
            if ("huggingface".equalsIgnoreCase(imageProvider)) {
                return generateWithHuggingFace(enhancedPrompt);
            } else {
                log.warn("Unknown image provider: {}, falling back to Hugging Face", imageProvider);
                return generateWithHuggingFace(enhancedPrompt);
            }
        } catch (Exception e) {
            log.error("Error generating AI image: {}", e.getMessage());
            throw e; // Re-throw for retry mechanism
        }
    }

    private String generateWithHuggingFace(String prompt) {
        if (huggingfaceApiKey == null || huggingfaceApiKey.isEmpty()) {
            log.warn("Hugging Face API key not configured");
            return null;
        }

        try {
            String apiUrl = "https://router.huggingface.co/hf-inference/models/stabilityai/stable-diffusion-xl-base-1.0";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + huggingfaceApiKey);
            headers.setAccept(Collections.singletonList(MediaType.IMAGE_PNG));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("inputs", prompt);
            requestBody.put("options", Map.of("wait_for_model", true));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                byte[].class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                byte[] imageBytes = response.getBody();
                String fileName = "ai_concept_" + System.currentTimeMillis();
                String supabaseUrl = supabaseStorageService.uploadImage(imageBytes, fileName);
                
                if (supabaseUrl != null) {
                    log.info("Successfully generated and uploaded image to Supabase");
                    return supabaseUrl;
                } else {
                    log.warn("Failed to upload to Supabase, falling back to base64");
                    String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                    return "data:image/png;base64," + base64Image;
                }
            }

            log.error("Failed to generate image with Hugging Face. Status: {}", response.getStatusCode());
            return null;

        } catch (Exception e) {
            log.error("Error calling Hugging Face API: {}", e.getMessage(), e);
            return null;
        }
    }

    private String enhancePromptForReligiousArt(String originalPrompt) {
        return "Catholic religious art, traditional style, peaceful and reverent: " + 
               originalPrompt + 
               ". High quality craftsmanship, detailed, beautiful, sacred art.";
    }
    
    @Override
    public String buildPromptFromTemplate(ProductTemplate template, Map<String, String> zoneInputs, String additionalDescription) {
        StringBuilder promptBuilder = new StringBuilder();
        
        // Start with base prompt hint from template
        if (template.getBasePromptHint() != null && !template.getBasePromptHint().isEmpty()) {
            promptBuilder.append(template.getBasePromptHint());
        } else {
            promptBuilder.append("Catholic religious souvenir");
        }
        
        // Add zone inputs
        if (zoneInputs != null && !zoneInputs.isEmpty() && template.getCustomZones() != null) {
            promptBuilder.append(". Customizations: ");
            
            List<String> zoneDescriptions = new ArrayList<>();
            for (TemplateCustomZone zone : template.getCustomZones()) {
                String zoneValue = zoneInputs.get(zone.getZoneId().toString());
                if (zoneValue != null && !zoneValue.isEmpty()) {
                    zoneDescriptions.add(zone.getZoneName() + ": " + zoneValue);
                }
            }
            promptBuilder.append(String.join(", ", zoneDescriptions));
        }
        
        // Add additional description
        if (additionalDescription != null && !additionalDescription.isEmpty()) {
            promptBuilder.append(". Additional details: ").append(additionalDescription);
        }
        
        // Add material and style if available
        if (template.getMaterial() != null && !template.getMaterial().isEmpty()) {
            promptBuilder.append(". Material: ").append(template.getMaterial());
        }
        
        if (template.getStyle() != null && !template.getStyle().isEmpty()) {
            promptBuilder.append(". Style: ").append(template.getStyle());
        }
        
        return promptBuilder.toString();
    }
    
    @Override
    public AIImageResponse generateConceptImage(AIPromptRequest request) {
        try {
            // Build the prompt
            StringBuilder promptBuilder = new StringBuilder();
            
            if (request.getBasePromptHint() != null && !request.getBasePromptHint().isEmpty()) {
                promptBuilder.append(request.getBasePromptHint());
            }
            
            if (request.getZoneInputs() != null && !request.getZoneInputs().isEmpty()) {
                promptBuilder.append(". Customizations: ");
                List<String> inputs = new ArrayList<>();
                for (Map.Entry<String, String> entry : request.getZoneInputs().entrySet()) {
                    if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                        inputs.add(entry.getValue());
                    }
                }
                promptBuilder.append(String.join(", ", inputs));
            }
            
            if (request.getAdditionalDescription() != null && !request.getAdditionalDescription().isEmpty()) {
                promptBuilder.append(". ").append(request.getAdditionalDescription());
            }
            
            String finalPrompt = promptBuilder.toString();
            log.info("Generating AI image with prompt: {}", finalPrompt);
            
            // Generate the image
            String imageUrl = generateImage(finalPrompt);
            
            if (imageUrl != null) {
                return AIImageResponse.builder()
                        .imageUrl(imageUrl)
                        .prompt(finalPrompt)
                        .success(true)
                        .build();
            } else {
                return AIImageResponse.builder()
                        .success(false)
                        .errorMessage("Failed to generate image. AI service may be unavailable.")
                        .prompt(finalPrompt)
                        .build();
            }
            
        } catch (Exception e) {
            log.error("Error generating concept image: {}", e.getMessage(), e);
            return AIImageResponse.builder()
                    .success(false)
                    .errorMessage("Error generating image: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * Async version of generateConceptImage for non-blocking operations
     */
    @Async("aiImageExecutor")
    public CompletableFuture<AIImageResponse> generateConceptImageAsync(AIPromptRequest request) {
        AIImageResponse response = generateConceptImage(request);
        return CompletableFuture.completedFuture(response);
    }
}
