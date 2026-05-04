package org.example.catholicsouvenircustomorder.service.imp;

import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.request.AIPromptRequest;
import org.example.catholicsouvenircustomorder.dto.response.AIImageResponse;
import org.example.catholicsouvenircustomorder.model.ProductTemplate;
import org.example.catholicsouvenircustomorder.model.TemplateCustomZone;
import org.example.catholicsouvenircustomorder.service.AIImageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class AIImageServiceImp implements AIImageService {
    
    @Value("${ai.image.mock-mode:false}")
    private boolean mockMode;

    private final CloudflareProvider cloudflareProvider;

    public AIImageServiceImp(CloudflareProvider cloudflareProvider) {
        this.cloudflareProvider = cloudflareProvider;
    }

    @Override
    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2),
        retryFor = {Exception.class}
    )
    public String generateImage(String prompt) {
        if (mockMode) {
            log.info("Mock mode enabled, returning placeholder image");
            return generatePlaceholderImage(prompt);
        }
        
        try {
            String enhancedPrompt = enhancePromptForReligiousArt(prompt);
            
            log.info("🎨 Using Cloudflare AI to generate image");
            String result = cloudflareProvider.generateImage(enhancedPrompt);
            
            if (result == null) {
                log.warn("⚠️ Cloudflare API unavailable, returning placeholder");
                return generatePlaceholderImage(prompt);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("❌ Error generating AI image: {}", e.getMessage());
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



    private String enhancePromptForReligiousArt(String originalPrompt) {
        // Only translate Vietnamese to English if needed
        // Don't add extra context that might override customer's specific requirements
        String translatedPrompt = translateToEnglishIfNeeded(originalPrompt);
        
        // Add minimal context only if prompt is very short (less than 5 words)
        String[] words = translatedPrompt.trim().split("\\s+");
        if (words.length < 5) {
            return "Catholic religious souvenir: " + translatedPrompt;
        }
        
        // For detailed prompts, use as-is to respect customer's specifications
        return translatedPrompt;
    }
    
    /**
     * Translate Vietnamese prompt to English for better AI image generation
     * Uses simple keyword mapping for common Catholic terms
     */
    private String translateToEnglishIfNeeded(String prompt) {
        // Check if prompt contains Vietnamese characters
        if (!containsVietnamese(prompt)) {
            return prompt;
        }
        
        log.info("📝 Translating Vietnamese prompt to English");
        
        // Common Vietnamese -> English mappings for Catholic items
        Map<String, String> translations = new HashMap<>();
        
        // Religious figures
        translations.put("đức mẹ", "Virgin Mary");
        translations.put("đức maria", "Virgin Mary");
        translations.put("mẹ maria", "Virgin Mary");
        translations.put("thánh mẫu", "Holy Mother");
        translations.put("chúa giêsu", "Jesus Christ");
        translations.put("chúa jesus", "Jesus Christ");
        translations.put("chúa kitô", "Jesus Christ");
        translations.put("thánh giuse", "Saint Joseph");
        translations.put("thánh joseph", "Saint Joseph");
        translations.put("thánh gioan", "Saint John");
        translations.put("thánh phêrô", "Saint Peter");
        translations.put("thánh phaolô", "Saint Paul");
        
        // Religious items
        translations.put("tượng", "statue");
        translations.put("thánh giá", "crucifix");
        translations.put("cây thánh giá", "holy cross");
        translations.put("thánh tích", "relic");
        translations.put("huy chương", "medal");
        translations.put("tràng hạt", "rosary");
        translations.put("chuỗi hạt", "rosary beads");
        translations.put("nhẫn", "ring");
        translations.put("dây chuyền", "necklace");
        translations.put("mặt dây chuyền", "pendant");
        translations.put("khung ảnh", "picture frame");
        translations.put("tranh", "painting");
        
        // Materials
        translations.put("gỗ", "wood");
        translations.put("gỗ sồi", "oak wood");
        translations.put("gỗ hương", "rosewood");
        translations.put("đồng", "bronze");
        translations.put("bạc", "silver");
        translations.put("vàng", "gold");
        translations.put("đá", "stone");
        translations.put("đá cẩm thạch", "marble");
        translations.put("sứ", "ceramic");
        translations.put("thủy tinh", "glass");
        translations.put("pha lê", "crystal");
        
        // Styles
        translations.put("cổ điển", "classical style");
        translations.put("hiện đại", "modern style");
        translations.put("truyền thống", "traditional style");
        translations.put("gothic", "gothic style");
        translations.put("baroque", "baroque style");
        translations.put("phong cách", "style");
        
        // Colors
        translations.put("màu xanh", "blue");
        translations.put("màu trắng", "white");
        translations.put("màu vàng", "golden");
        translations.put("màu đỏ", "red");
        translations.put("màu nâu", "brown");
        
        // Attributes
        translations.put("cao", "tall");
        translations.put("lớn", "large");
        translations.put("nhỏ", "small");
        translations.put("đẹp", "beautiful");
        translations.put("trang nhã", "elegant");
        translations.put("tinh xảo", "intricate");
        translations.put("chi tiết", "detailed");
        translations.put("trang trí", "decorated");
        translations.put("khắc", "carved");
        translations.put("chạm khắc", "engraved");
        
        // Religious concepts
        translations.put("thiên chúa", "God");
        translations.put("thánh thần", "Holy Spirit");
        translations.put("thiên thần", "angel");
        translations.put("thánh", "saint");
        translations.put("phép lạ", "miracle");
        translations.put("phước lành", "blessing");
        translations.put("cầu nguyện", "prayer");
        translations.put("nhà thờ", "church");
        translations.put("nhà nguyện", "chapel");
        
        // Occasions
        translations.put("rửa tội", "baptism");
        translations.put("thêm sức", "confirmation");
        translations.put("hôn phối", "wedding");
        translations.put("cưới", "wedding");
        translations.put("giáng sinh", "Christmas");
        translations.put("phục sinh", "Easter");
        translations.put("lễ", "feast");
        
        // Common phrases
        translations.put("với", "with");
        translations.put("và", "and");
        translations.put("có", "with");
        translations.put("áo choàng", "robe");
        translations.put("vương miện", "crown");
        translations.put("hào quang", "halo");
        translations.put("tia sáng", "rays of light");
        
        String result = prompt.toLowerCase();
        
        // Apply translations
        for (Map.Entry<String, String> entry : translations.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        
        log.info("🔄 Original: {}", prompt);
        log.info("🔄 Translated: {}", result);
        
        return result;
    }
    
    /**
     * Check if string contains Vietnamese characters
     */
    private boolean containsVietnamese(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        // Vietnamese specific characters
        String vietnameseChars = "àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ" +
                                "ÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ";
        
        for (char c : text.toCharArray()) {
            if (vietnameseChars.indexOf(c) >= 0) {
                return true;
            }
        }
        
        return false;
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
