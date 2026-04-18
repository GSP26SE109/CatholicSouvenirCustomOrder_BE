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
                String result = generateWithHuggingFace(enhancedPrompt);
                
                // If Hugging Face fails, return a placeholder
                if (result == null) {
                    log.warn("Hugging Face API unavailable, returning placeholder");
                    return generatePlaceholderImage(prompt);
                }
                
                return result;
            } else {
                log.warn("Unknown image provider: {}, falling back to Hugging Face", imageProvider);
                String result = generateWithHuggingFace(enhancedPrompt);
                
                if (result == null) {
                    return generatePlaceholderImage(prompt);
                }
                
                return result;
            }
        } catch (Exception e) {
            log.error("Error generating AI image: {}", e.getMessage());
            return generatePlaceholderImage(prompt);
        }
    }
    
    /**
     * Generate a placeholder image when AI service is unavailable
     */
    private String generatePlaceholderImage(String prompt) {
        // Return a professional-looking SVG placeholder
        String escapedPrompt = prompt.replace("&", "&amp;")
                                    .replace("<", "&lt;")
                                    .replace(">", "&gt;")
                                    .replace("\"", "&quot;");
        
        String displayPrompt = escapedPrompt.length() > 60 ? 
            escapedPrompt.substring(0, 60) + "..." : escapedPrompt;
        
        String svg = String.format(
            "<svg width='1024' height='1024' xmlns='http://www.w3.org/2000/svg'>" +
            "<defs>" +
            "<linearGradient id='grad' x1='0%%' y1='0%%' x2='100%%' y2='100%%'>" +
            "<stop offset='0%%' style='stop-color:%s;stop-opacity:1' />" +
            "<stop offset='100%%' style='stop-color:%s;stop-opacity:1' />" +
            "</linearGradient>" +
            "</defs>" +
            "<rect width='1024' height='1024' fill='url(#grad)'/>" +
            
            // Icon placeholder
            "<circle cx='512' cy='400' r='80' fill='white' opacity='0.2'/>" +
            "<path d='M 512 360 L 512 440 M 472 400 L 552 400' stroke='white' stroke-width='8' opacity='0.3'/>" +
            
            // Title
            "<text x='512' y='550' font-family='Arial, sans-serif' font-size='24' font-weight='bold' " +
            "fill='white' text-anchor='middle'>AI Image Generation</text>" +
            
            // Status
            "<text x='512' y='590' font-family='Arial, sans-serif' font-size='18' " +
            "fill='white' text-anchor='middle' opacity='0.8'>Service Temporarily Unavailable</text>" +
            
            // Prompt preview
            "<text x='512' y='650' font-family='Arial, sans-serif' font-size='14' " +
            "fill='white' text-anchor='middle' opacity='0.6'>%s</text>" +
            
            "</svg>",
            "#4F46E5", // Indigo
            "#7C3AED", // Purple
            displayPrompt
        );
        
        String base64Svg = Base64.getEncoder().encodeToString(svg.getBytes());
        return "data:image/svg+xml;base64," + base64Svg;
    }

    private String generateWithHuggingFace(String prompt) {
        if (huggingfaceApiKey == null || huggingfaceApiKey.isEmpty()) {
            log.warn("Hugging Face API key not configured");
            return null;
        }

        // List of models to try in order of preference
        String[] models = {
            "stabilityai/stable-diffusion-xl-base-1.0",
            "stabilityai/stable-diffusion-2-1",
            "runwayml/stable-diffusion-v1-5",
            "CompVis/stable-diffusion-v1-4",
            "prompthero/openjourney"
        };

        for (String model : models) {
            try {
                log.info("Attempting to generate image with model: {}", model);
                String apiUrl = "https://api-inference.huggingface.co/models/" + model;
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + huggingfaceApiKey);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("inputs", prompt);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<byte[]> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    byte[].class
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    byte[] imageBytes = response.getBody();
                    
                    // Check if response is actually an image (not JSON error)
                    if (imageBytes.length > 100) {
                        String fileName = "ai_concept_" + System.currentTimeMillis();
                        String supabaseUrl = supabaseStorageService.uploadImage(imageBytes, fileName);
                        
                        if (supabaseUrl != null) {
                            log.info("Successfully generated and uploaded image to Supabase using model: {}", model);
                            return supabaseUrl;
                        } else {
                            log.warn("Failed to upload to Supabase, falling back to base64");
                            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                            return "data:image/png;base64," + base64Image;
                        }
                    }
                }

                log.warn("Model {} returned invalid response, trying next model", model);

            } catch (Exception e) {
                log.warn("Failed with model {}: {}. Trying next model...", model, e.getMessage());
            }
        }

        log.error("All Hugging Face models failed to generate image");
        return null;
    }

    private String enhancePromptForReligiousArt(String originalPrompt) {
        return "Catholic religious art, traditional style, peaceful and reverent: " + 
               originalPrompt + 
               ". High quality craftsmanship, detailed, beautiful, sacred art.";
    }
    
    @Override
    public String buildPromptFromTemplate(ProductTemplate template, Map<String, String> zoneInputs, String additionalDescription) {
        StringBuilder promptBuilder = new StringBuilder();
        
        // Start with template name and description
        promptBuilder.append("Catholic religious souvenir: ");
        promptBuilder.append(template.getName());
        if (template.getDescription() != null && !template.getDescription().isEmpty()) {
            promptBuilder.append(". ").append(template.getDescription());
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
        
        return promptBuilder.toString();
    }
    
    @Override
    public AIImageResponse generateConceptImage(AIPromptRequest request) {
        try {
            // Build the prompt
            StringBuilder promptBuilder = new StringBuilder();
            
            promptBuilder.append("Catholic religious souvenir");
            
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
