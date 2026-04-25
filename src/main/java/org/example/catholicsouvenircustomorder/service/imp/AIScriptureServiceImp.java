package org.example.catholicsouvenircustomorder.service.imp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.request.ScriptureRecommendRequest;
import org.example.catholicsouvenircustomorder.dto.response.ScriptureRecommendResponse;
import org.example.catholicsouvenircustomorder.dto.response.ScriptureRecommendation;
import org.example.catholicsouvenircustomorder.service.AIScriptureService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class AIScriptureServiceImp implements AIScriptureService {

    @Value("${huggingface.api.key:}")
    private String huggingfaceApiKey;

    @Value("${huggingface.text.model:meta-llama/Llama-3.1-8B-Instruct:novita}")
    private String huggingfaceTextModel;

    @Value("${huggingface.api.url:https://router.huggingface.co/v1/chat/completions}")
    private String huggingfaceApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ScriptureRecommendResponse recommendScriptures(ScriptureRecommendRequest request) {
        try {
            log.info("Recommending scriptures for purpose: {}, product: {}, theme: {}", 
                    request.getPurpose(), request.getProductName(), request.getTheme());

            // Try AI first if API key is configured
            if (huggingfaceApiKey != null && !huggingfaceApiKey.isEmpty()) {
                try {
                    String prompt = buildScripturePrompt(request);
                    String aiResponse = callHuggingFace(prompt);
                    List<ScriptureRecommendation> recommendations = parseScriptureResponse(aiResponse);

                    if (!recommendations.isEmpty()) {
                        log.info("AI recommendation successful, returning {} scriptures", recommendations.size());
                        return ScriptureRecommendResponse.builder()
                                .success(true)
                                .message("Gợi ý câu Kinh Thánh thành công (AI)")
                                .recommendations(recommendations)
                                .build();
                    }
                } catch (Exception e) {
                    log.warn("AI service unavailable, using curated scriptures: {}", e.getMessage());
                }
            }
            
            // Fallback to curated scriptures
            return getCuratedScriptures(request);

        } catch (Exception e) {
            log.error("Error recommending scriptures: {}", e.getMessage(), e);
            return getCuratedScriptures(request);
        }
    }

    @Override
    public ScriptureRecommendResponse getPopularScripturesForOccasion(String occasion, String language) {
        try {
            log.info("Getting popular scriptures for occasion: {}, language: {}", occasion, language);

            // Predefined popular scriptures for common occasions
            List<ScriptureRecommendation> recommendations = getPopularScripturesByOccasion(occasion, language);

            return ScriptureRecommendResponse.builder()
                    .success(true)
                    .message("Lấy câu Kinh Thánh phổ biến thành công")
                    .recommendations(recommendations)
                    .build();

        } catch (Exception e) {
            log.error("Error getting popular scriptures: {}", e.getMessage(), e);
            return ScriptureRecommendResponse.builder()
                    .success(false)
                    .errorMessage("Không thể lấy câu Kinh Thánh. Vui lòng thử lại sau.")
                    .recommendations(new ArrayList<>())
                    .build();
        }
    }

    private String buildScripturePrompt(ScriptureRecommendRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        String lang = request.getLanguage() != null ? request.getLanguage() : "en";
        
        // Build prompt based on language
        if ("vi".equals(lang)) {
            prompt.append("Hãy gợi ý ");
            prompt.append(request.getMaxResults() != null ? request.getMaxResults() : 3);
            prompt.append(" câu Kinh Thánh phù hợp cho sản phẩm lưu niệm Công Giáo.\n\n");
            
            prompt.append("Mục đích: ").append(request.getPurpose()).append("\n");
            
            if (request.getProductName() != null && !request.getProductName().isEmpty()) {
                prompt.append("Sản phẩm: ").append(request.getProductName()).append("\n");
            }
            
            if (request.getTheme() != null && !request.getTheme().isEmpty()) {
                prompt.append("Chủ đề: ").append(request.getTheme()).append("\n");
            }
            
            prompt.append("\nVui lòng trả về kết quả HOÀN TOÀN BẰNG TIẾNG VIỆT với format:\n");
            prompt.append("Câu | Nội dung tiếng Việt | Lý do | Dịp phù hợp\n\n");
            prompt.append("Ví dụ: Gioan 3:16 | Vì Thiên Chúa yêu thế gian đến nỗi đã ban Con Một của Người | Thể hiện tình yêu vô bờ của Thiên Chúa | Mọi dịp\n");
            prompt.append("\nLưu ý: Tất cả nội dung phải bằng tiếng Việt, sử dụng bản dịch Kinh Thánh Công Giáo.");
        } else {
            // English prompt
            prompt.append("Recommend ");
            prompt.append(request.getMaxResults() != null ? request.getMaxResults() : 3);
            prompt.append(" Bible verses for Catholic souvenir engraving.\n\n");
            
            prompt.append("Purpose: ").append(request.getPurpose()).append("\n");
            
            if (request.getProductName() != null && !request.getProductName().isEmpty()) {
                prompt.append("Product: ").append(request.getProductName()).append("\n");
            }
            
            if (request.getTheme() != null && !request.getTheme().isEmpty()) {
                prompt.append("Theme: ").append(request.getTheme()).append("\n");
            }
            
            prompt.append("\nProvide results in format:\n");
            prompt.append("Verse | Text | Reason | Occasion\n\n");
            prompt.append("Example: John 3:16 | For God so loved the world... | Shows God's infinite love | All occasions");
        }
        
        return prompt.toString();
    }

    private String callHuggingFace(String prompt) throws Exception {
        // Use Chat Completions API (OpenAI-compatible format)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(huggingfaceApiKey);

        // Build messages array
        List<Map<String, String>> messages = List.of(
            Map.of("role", "system", "content", "You are a Catholic scripture expert. Provide Bible verse recommendations."),
            Map.of("role", "user", "content", prompt)
        );

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", huggingfaceTextModel);
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 500);
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
                // Parse Chat Completions response
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

    private List<ScriptureRecommendation> parseScriptureResponse(String aiResponse) {
        List<ScriptureRecommendation> recommendations = new ArrayList<>();
        
        try {
            // Parse format: Verse | Text | Reason | Occasion
            String[] lines = aiResponse.split("\n");
            
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.length() < 10) continue;
                
                // Try to parse pipe-separated format
                String[] parts = line.split("\\|");
                if (parts.length >= 3) {
                    recommendations.add(ScriptureRecommendation.builder()
                            .verse(parts[0].trim())
                            .text(parts.length > 1 ? parts[1].trim() : "")
                            .reason(parts.length > 2 ? parts[2].trim() : "")
                            .occasion(parts.length > 3 ? parts[3].trim() : "")
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse AI response: {}", e.getMessage());
        }
        
        return recommendations;
    }

    private ScriptureRecommendResponse getCuratedScriptures(ScriptureRecommendRequest request) {
        List<ScriptureRecommendation> recommendations = new ArrayList<>();
        
        // Smart matching based on purpose/theme
        String purpose = request.getPurpose() != null ? request.getPurpose().toLowerCase() : "";
        String theme = request.getTheme() != null ? request.getTheme().toLowerCase() : "";
        String combined = purpose + " " + theme;
        
        // Baptism related
        if (combined.contains("baptism") || combined.contains("rửa tội") || combined.contains("baptême")) {
            recommendations.addAll(getBaptismScriptures());
        }
        // Wedding related
        else if (combined.contains("wedding") || combined.contains("marriage") || combined.contains("cưới") || 
                 combined.contains("love") || combined.contains("tình yêu")) {
            recommendations.addAll(getWeddingScriptures());
        }
        // Christmas related
        else if (combined.contains("christmas") || combined.contains("giáng sinh") || combined.contains("noel")) {
            recommendations.addAll(getChristmasScriptures());
        }
        // Faith/Hope/Protection
        else if (combined.contains("faith") || combined.contains("hope") || combined.contains("protection") ||
                 combined.contains("đức tin") || combined.contains("hy vọng") || combined.contains("bảo vệ")) {
            recommendations.addAll(getFaithScriptures());
        }
        // Default general scriptures
        else {
            recommendations.addAll(getGeneralScriptures());
        }
        
        // Limit to maxResults
        int maxResults = request.getMaxResults() != null ? request.getMaxResults() : 3;
        if (recommendations.size() > maxResults) {
            recommendations = recommendations.subList(0, maxResults);
        }
        
        return ScriptureRecommendResponse.builder()
                .success(true)
                .message("Gợi ý câu Kinh Thánh (curated collection)")
                .recommendations(recommendations)
                .build();
    }
    
    private List<ScriptureRecommendation> getBaptismScriptures() {
        return List.of(
            ScriptureRecommendation.builder()
                    .verse("Matthew 28:19")
                    .text("Go therefore and make disciples of all nations, baptizing them in the name of the Father and of the Son and of the Holy Spirit")
                    .translation("Vậy anh em hãy đi và làm cho muôn dân trở nên môn đệ, làm phép rửa cho họ nhân danh Cha và Con và Thánh Thần")
                    .reason("The Great Commission - foundation of Baptism")
                    .occasion("Baptism, Confirmation")
                    .build(),
            ScriptureRecommendation.builder()
                    .verse("Romans 6:4")
                    .text("We were therefore buried with him through baptism into death")
                    .translation("Chúng ta đã được mai táng với Người qua phép rửa để cùng chết")
                    .reason("Explains the spiritual meaning of Baptism")
                    .occasion("Baptism")
                    .build(),
            ScriptureRecommendation.builder()
                    .verse("John 3:5")
                    .text("No one can enter the kingdom of God unless they are born of water and the Spirit")
                    .translation("Không ai có thể vào nước Thiên Chúa nếu không sinh bởi nước và Thánh Thần")
                    .reason("Jesus' teaching on the necessity of Baptism")
                    .occasion("Baptism")
                    .build()
        );
    }
    
    private List<ScriptureRecommendation> getWeddingScriptures() {
        return List.of(
            ScriptureRecommendation.builder()
                    .verse("1 Corinthians 13:4-7")
                    .text("Love is patient, love is kind. It does not envy, it does not boast")
                    .translation("Tình yêu thì kiên nhẫn, tử tế; không ghen tị, không khoe khoang")
                    .reason("The most famous passage about love")
                    .occasion("Wedding, Anniversary")
                    .build(),
            ScriptureRecommendation.builder()
                    .verse("Genesis 2:24")
                    .text("Therefore a man shall leave his father and mother and be joined to his wife")
                    .translation("Vì thế, người nam sẽ lìa cha mẹ mà gắn bó với vợ mình")
                    .reason("Foundation of marriage")
                    .occasion("Wedding")
                    .build(),
            ScriptureRecommendation.builder()
                    .verse("Ephesians 5:25")
                    .text("Husbands, love your wives, just as Christ loved the church")
                    .translation("Hỡi những người chồng, hãy yêu vợ mình như Chúa Kitô đã yêu Hội Thánh")
                    .reason("Model of sacrificial love in marriage")
                    .occasion("Wedding, Anniversary")
                    .build()
        );
    }
    
    private List<ScriptureRecommendation> getChristmasScriptures() {
        return List.of(
            ScriptureRecommendation.builder()
                    .verse("Luke 2:11")
                    .text("Today in the town of David a Savior has been born to you; he is the Messiah, the Lord")
                    .translation("Hôm nay, trong thành vua Đavít, một Đấng Cứu Độ đã sinh ra cho anh em")
                    .reason("The announcement of Jesus' birth")
                    .occasion("Christmas")
                    .build(),
            ScriptureRecommendation.builder()
                    .verse("John 1:14")
                    .text("The Word became flesh and made his dwelling among us")
                    .translation("Ngôi Lời đã trở nên người phàm và cư ngụ giữa chúng ta")
                    .reason("The Incarnation - core of Christmas")
                    .occasion("Christmas")
                    .build(),
            ScriptureRecommendation.builder()
                    .verse("Isaiah 9:6")
                    .text("For to us a child is born, to us a son is given")
                    .translation("Vì một trẻ em đã sinh ra cho chúng ta, một con trai đã được ban cho chúng ta")
                    .reason("Prophecy of the Messiah's birth")
                    .occasion("Christmas")
                    .build()
        );
    }
    
    private List<ScriptureRecommendation> getFaithScriptures() {
        return List.of(
            ScriptureRecommendation.builder()
                    .verse("Philippians 4:13")
                    .text("I can do all things through Christ who strengthens me")
                    .translation("Tôi có sức mạnh làm mọi sự nhờ Đấng ban sức cho tôi")
                    .reason("Message of strength and faith")
                    .occasion("Confirmation, difficult times, encouragement")
                    .build(),
            ScriptureRecommendation.builder()
                    .verse("Psalm 23:1")
                    .text("The Lord is my shepherd, I lack nothing")
                    .translation("Chúa chăn dắt tôi, tôi chẳng thiếu thốn gì")
                    .reason("Message of trust and divine providence")
                    .occasion("All occasions, protection items")
                    .build(),
            ScriptureRecommendation.builder()
                    .verse("Proverbs 3:5-6")
                    .text("Trust in the Lord with all your heart and lean not on your own understanding")
                    .translation("Hãy tin cậy Chúa hết lòng, đừng dựa vào sự hiểu biết của mình")
                    .reason("Encouragement to trust in God")
                    .occasion("All occasions, guidance")
                    .build()
        );
    }
    
    private List<ScriptureRecommendation> getGeneralScriptures() {
        return List.of(
            ScriptureRecommendation.builder()
                    .verse("John 3:16")
                    .text("For God so loved the world that he gave his one and only Son")
                    .translation("Vì Thiên Chúa yêu thế gian đến nỗi đã ban Con Một của Người")
                    .reason("Universal message of God's love")
                    .occasion("All occasions")
                    .build(),
            ScriptureRecommendation.builder()
                    .verse("Philippians 4:13")
                    .text("I can do all things through Christ who strengthens me")
                    .translation("Tôi có sức mạnh làm mọi sự nhờ Đấng ban sức cho tôi")
                    .reason("Message of strength and faith")
                    .occasion("All occasions")
                    .build(),
            ScriptureRecommendation.builder()
                    .verse("Psalm 23:1")
                    .text("The Lord is my shepherd, I lack nothing")
                    .translation("Chúa chăn dắt tôi, tôi chẳng thiếu thốn gì")
                    .reason("Message of trust and divine providence")
                    .occasion("All occasions")
                    .build()
        );
    }

    private ScriptureRecommendResponse getFallbackScriptures(ScriptureRecommendRequest request) {
        return getCuratedScriptures(request);
    }

    private List<ScriptureRecommendation> getPopularScripturesByOccasion(String occasion, String language) {
        List<ScriptureRecommendation> recommendations = new ArrayList<>();
        
        switch (occasion.toLowerCase()) {
            case "baptism", "baptême", "rửa tội" -> {
                recommendations.add(ScriptureRecommendation.builder()
                        .verse("Matthew 28:19")
                        .text("Go therefore and make disciples of all nations, baptizing them in the name of the Father and of the Son and of the Holy Spirit")
                        .translation("Vậy anh em hãy đi và làm cho muôn dân trở nên môn đệ, làm phép rửa cho họ nhân danh Cha và Con và Thánh Thần")
                        .reason("The Great Commission - foundation of Baptism")
                        .occasion("Baptism")
                        .build());
                
                recommendations.add(ScriptureRecommendation.builder()
                        .verse("Romans 6:4")
                        .text("We were therefore buried with him through baptism into death")
                        .translation("Chúng ta đã được mai táng với Người qua phép rửa để cùng chết")
                        .reason("Explains the meaning of Baptism")
                        .occasion("Baptism")
                        .build());
            }
            
            case "wedding", "marriage", "cưới", "hôn nhân" -> {
                recommendations.add(ScriptureRecommendation.builder()
                        .verse("1 Corinthians 13:4-7")
                        .text("Love is patient, love is kind...")
                        .translation("Tình yêu thì kiên nhẫn, tử tế...")
                        .reason("The most famous passage about love")
                        .occasion("Wedding, Anniversary")
                        .build());
                
                recommendations.add(ScriptureRecommendation.builder()
                        .verse("Genesis 2:24")
                        .text("Therefore a man shall leave his father and mother and be joined to his wife")
                        .translation("Vì thế, người nam sẽ lìa cha mẹ mà gắn bó với vợ mình")
                        .reason("Foundation of marriage")
                        .occasion("Wedding")
                        .build());
            }
            
            case "christmas", "noel", "giáng sinh" -> {
                recommendations.add(ScriptureRecommendation.builder()
                        .verse("Luke 2:11")
                        .text("Today in the town of David a Savior has been born to you; he is the Messiah, the Lord")
                        .translation("Hôm nay, trong thành vua Đavít, một Đấng Cứu Độ đã sinh ra cho anh em, Người là Đấng Kitô, là Chúa")
                        .reason("The announcement of Jesus' birth")
                        .occasion("Christmas")
                        .build());
                
                recommendations.add(ScriptureRecommendation.builder()
                        .verse("John 1:14")
                        .text("The Word became flesh and made his dwelling among us")
                        .translation("Ngôi Lời đã trở nên người phàm và cư ngụ giữa chúng ta")
                        .reason("The Incarnation - core of Christmas")
                        .occasion("Christmas")
                        .build());
            }
            
            default -> {
                // Return general popular verses
                recommendations.addAll(getFallbackScriptures(null).getRecommendations());
            }
        }
        
        return recommendations;
    }
}
