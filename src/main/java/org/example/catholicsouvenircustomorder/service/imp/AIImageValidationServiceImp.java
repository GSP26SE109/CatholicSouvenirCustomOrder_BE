package org.example.catholicsouvenircustomorder.service.imp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.response.ImageValidationResponse;
import org.example.catholicsouvenircustomorder.service.AIImageValidationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@Slf4j
public class AIImageValidationServiceImp implements AIImageValidationService {

    @Value("${huggingface.api.key:}")
    private String huggingfaceApiKey;

    @Value("${huggingface.api.url:https://router.huggingface.co/v1/chat/completions}")
    private String huggingfaceApiUrl;

    @Value("${huggingface.vision.model:Qwen/Qwen2.5-VL-72B-Instruct}")
    private String huggingfaceVisionModel;
    
    @Value("${huggingface.text.model:meta-llama/Llama-3.1-8B-Instruct:novita}")
    private String huggingfaceTextModel;

    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.8;
    private static final double MEDIUM_CONFIDENCE_THRESHOLD = 0.5;

    private static final Set<String> CATHOLIC_KEYWORDS = Set.of(
            "cross", "crucifix", "rosary", "statue", "mary", "jesus", "christ",
            "saint", "angel", "church", "chapel", "bible", "prayer", "holy",
            "blessed", "sacred", "religious", "catholic", "christian", "madonna",
            "virgin", "chalice", "monstrance", "altar", "candle", "icon"
    );

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ImageValidationResponse validateCatholicImage(MultipartFile image) {
        try {
            log.info("Đang xác thực hình ảnh Công Giáo: {}", image.getOriginalFilename());

            if (image.isEmpty()) {
                return createInvalidResponse("File hình ảnh trống");
            }

            if (!isValidImageType(image.getContentType())) {
                return createInvalidResponse("Định dạng hình ảnh không hợp lệ. Chỉ chấp nhận JPG, PNG, WEBP");
            }

            if (huggingfaceApiKey != null && !huggingfaceApiKey.isEmpty()) {
                try {
                    String base64Image = Base64.getEncoder().encodeToString(image.getBytes());
                    return analyzeImageWithAI(base64Image, image.getOriginalFilename());
                } catch (Exception e) {
                    log.warn("Xác thực AI thất bại: {}", e.getMessage());
                }
            }

            return validateByFilename(image.getOriginalFilename());

        } catch (Exception e) {
            log.error("Lỗi khi xác thực hình ảnh: {}", e.getMessage(), e);
            return createInvalidResponse("Lỗi khi xác thực hình ảnh");
        }
    }

    @Override
    public ImageValidationResponse validateCatholicImageByUrl(String imageUrl) {
        try {
            log.info("Đang xác thực URL hình ảnh: {}", imageUrl);

            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                return createInvalidResponse("URL hình ảnh trống");
            }

            if (huggingfaceApiKey != null && !huggingfaceApiKey.isEmpty()) {
                try {
                    return analyzeImageUrlWithAI(imageUrl);
                } catch (Exception e) {
                    log.warn("Xác thực AI thất bại: {}", e.getMessage());
                }
            }

            return validateByFilename(imageUrl);

        } catch (Exception e) {
            log.error("Lỗi khi xác thực URL hình ảnh: {}", e.getMessage(), e);
            return createInvalidResponse("Lỗi khi xác thực hình ảnh");
        }
    }

    private ImageValidationResponse analyzeImageWithAI(String base64Image, String filename) {
        try {
            String prompt = buildImageAnalysisPrompt(filename);
            String aiResponse = callHuggingFaceVision(prompt, base64Image);
            return parseAIResponse(aiResponse);
        } catch (Exception e) {
            log.error("Phân tích AI thất bại: {}", e.getMessage());
            return validateByFilename(filename);
        }
    }

    private ImageValidationResponse analyzeImageUrlWithAI(String imageUrl) {
        try {
            String prompt = buildImageAnalysisPrompt(imageUrl);
            String aiResponse = callHuggingFaceVisionUrl(prompt, imageUrl);
            return parseAIResponse(aiResponse);
        } catch (Exception e) {
            log.error("Phân tích AI URL thất bại: {}", e.getMessage());
            return validateByFilename(imageUrl);
        }
    }

    private String buildImageAnalysisPrompt(String context) {
        return "Phân tích hình ảnh này và xác định xem có chứa vật phẩm tôn giáo Công Giáo hay không.\n\n" +
                "Trả lời theo định dạng JSON:\n" +
                "{\n" +
                "  \"isValid\": true/false,\n" +
                "  \"confidence\": 0.0-1.0,\n" +
                "  \"category\": \"statue/rosary/cross/religious_art/other\",\n" +
                "  \"detectedItems\": [\"item1\", \"item2\"],\n" +
                "  \"reasoning\": \"giải thích\"\n" +
                "}\n\n" +
                "Vật phẩm Công Giáo bao gồm: tượng thánh, thánh giá, chuỗi mân côi, huy chương thánh, " +
                "tranh tôn giáo, biểu tượng, chén thánh, nến thánh, v.v.\n\n" +
                "Hãy phân tích kỹ hình ảnh và đưa ra đánh giá chính xác.";
    }

    /**
     * Call Hugging Face Vision API with base64 image
     */
    private String callHuggingFaceVision(String prompt, String base64Image) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(huggingfaceApiKey);

        // Build messages with image content
        List<Map<String, Object>> messages = new ArrayList<>();
        
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        
        // Content array with text and image
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "text", "text", prompt));
        content.add(Map.of(
            "type", "image_url",
            "image_url", Map.of("url", "data:image/jpeg;base64," + base64Image)
        ));
        
        userMessage.put("content", content);
        messages.add(userMessage);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", huggingfaceVisionModel);
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 300);
        requestBody.put("temperature", 0.3);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                huggingfaceApiUrl,
                HttpMethod.POST,
                entity,
                String.class
        );

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();
        }

        throw new Exception("API trả về status không OK: " + response.getStatusCode());
    }

    /**
     * Call Hugging Face Vision API with image URL
     */
    private String callHuggingFaceVisionUrl(String prompt, String imageUrl) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(huggingfaceApiKey);

        // Build messages with image URL
        List<Map<String, Object>> messages = new ArrayList<>();
        
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        
        // Content array with text and image URL
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "text", "text", prompt));
        content.add(Map.of(
            "type", "image_url",
            "image_url", Map.of("url", imageUrl)
        ));
        
        userMessage.put("content", content);
        messages.add(userMessage);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", huggingfaceVisionModel);
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 300);
        requestBody.put("temperature", 0.3);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                huggingfaceApiUrl,
                HttpMethod.POST,
                entity,
                String.class
        );

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();
        }

        throw new Exception("API trả về status không OK: " + response.getStatusCode());
    }

    private ImageValidationResponse parseAIResponse(String aiResponse) {
        try {
            JsonNode json = objectMapper.readTree(aiResponse);
            boolean isValid = json.path("isValid").asBoolean(false);
            double confidence = json.path("confidence").asDouble(0.0);
            String category = json.path("category").asText("unknown");
            String reasoning = json.path("reasoning").asText("");

            List<String> detectedItems = new ArrayList<>();
            JsonNode itemsNode = json.path("detectedItems");
            if (itemsNode.isArray()) {
                itemsNode.forEach(item -> detectedItems.add(item.asText()));
            }

            return buildValidationResponse(isValid, confidence, category, detectedItems, reasoning);
        } catch (Exception e) {
            log.warn("Failed to parse AI response: {}", e.getMessage());
            return analyzeTextResponse(aiResponse);
        }
    }

    private ImageValidationResponse analyzeTextResponse(String response) {
        String lower = response.toLowerCase();
        int matchCount = 0;
        List<String> detectedItems = new ArrayList<>();

        for (String keyword : CATHOLIC_KEYWORDS) {
            if (lower.contains(keyword)) {
                matchCount++;
                detectedItems.add(keyword);
            }
        }

        double confidence = Math.min(1.0, matchCount / 5.0);
        boolean isValid = confidence >= MEDIUM_CONFIDENCE_THRESHOLD;

        return buildValidationResponse(isValid, confidence, detectCategory(detectedItems), 
                detectedItems, "Phân tích dựa trên từ khóa");
    }

    private ImageValidationResponse validateByFilename(String filename) {
        if (filename == null) {
            return createInvalidResponse("Tên file null");
        }

        String lower = filename.toLowerCase();
        List<String> detectedItems = new ArrayList<>();
        int matchCount = 0;

        for (String keyword : CATHOLIC_KEYWORDS) {
            if (lower.contains(keyword)) {
                matchCount++;
                detectedItems.add(keyword);
            }
        }

        if (matchCount > 0) {
            double confidence = Math.min(0.7, matchCount / 3.0);
            return buildValidationResponse(true, confidence, detectCategory(detectedItems), 
                    detectedItems, "Phân tích tên file (AI không khả dụng)");
        }

        return ImageValidationResponse.builder()
                .isValid(false)
                .confidenceScore(0.0)
                .category("unknown")
                .detectedItems(new ArrayList<>())
                .message("Không phát hiện vật phẩm Công Giáo")
                .warning("Cần xem xét thủ công")
                .requiresManualReview(true)
                .build();
    }

    private ImageValidationResponse buildValidationResponse(boolean isValid, double confidence, 
            String category, List<String> detectedItems, String reasoning) {
        String message;
        String warning = null;
        boolean requiresManualReview = false;

        if (!isValid) {
            message = "Hình ảnh không chứa vật phẩm Công Giáo";
            requiresManualReview = true;
        } else if (confidence >= HIGH_CONFIDENCE_THRESHOLD) {
            message = "Hình ảnh hợp lệ - Độ tin cậy cao";
        } else if (confidence >= MEDIUM_CONFIDENCE_THRESHOLD) {
            message = "Hình ảnh hợp lệ - Độ tin cậy trung bình";
            warning = "Nên xem xét thủ công";
            requiresManualReview = true;
        } else {
            message = "Xác thực hình ảnh không chắc chắn";
            warning = "Cần xem xét thủ công";
            requiresManualReview = true;
        }

        return ImageValidationResponse.builder()
                .isValid(isValid)
                .confidenceScore(confidence)
                .category(category)
                .detectedItems(detectedItems)
                .message(message + ". " + reasoning)
                .warning(warning)
                .requiresManualReview(requiresManualReview)
                .build();
    }

    private ImageValidationResponse createInvalidResponse(String message) {
        return ImageValidationResponse.builder()
                .isValid(false)
                .confidenceScore(0.0)
                .category("invalid")
                .detectedItems(new ArrayList<>())
                .message(message)
                .requiresManualReview(true)
                .build();
    }

    private String detectCategory(List<String> detectedItems) {
        if (detectedItems.isEmpty()) return "unknown";
        String first = detectedItems.get(0).toLowerCase();
        if (first.contains("statue")) return "statue";
        if (first.contains("rosary")) return "rosary";
        if (first.contains("cross")) return "cross";
        if (first.contains("icon")) return "religious_art";
        return "religious_item";
    }

    private boolean isValidImageType(String contentType) {
        if (contentType == null) return false;
        return contentType.equals("image/jpeg") || contentType.equals("image/jpg") ||
                contentType.equals("image/png") || contentType.equals("image/webp");
    }
}
