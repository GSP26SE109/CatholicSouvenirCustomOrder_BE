package org.example.catholicsouvenircustomorder.service.imp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.service.ChatBoxService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatboxServiceImp implements ChatBoxService {
    private final RestTemplate restTemplate;

    private final String API_URL =
            "https://api.groq.com/openai/v1/chat/completions";

    @Value("${ai.groq.api_key:}")
    private String api_key;

    @Override
    public String askAI(String userInput) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(api_key);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("model", "llama-3.3-70b-versatile");
            body.put("messages", List.of(
                    Map.of("role", "system", "content",
                            """
                            Bạn là một trợ lý Công giáo bằng tiếng Việt.
    
                            Nhiệm vụ:
                            - Trả lời bằng tiếng Việt tự nhiên, rõ ràng, dễ hiểu.
                            - Dựa trên Kinh Thánh, Giáo lý Hội Thánh Công giáo,
                              và truyền thống của Hội Thánh.
                            - Giữ giọng văn tôn trọng, hiền hòa, mang tính mục vụ.
                            - Nếu câu hỏi nhạy cảm hoặc liên quan tín lý,
                              hãy trả lời cẩn trọng và trung lập.
                            - Nếu chưa chắc chắn, hãy nói rõ và khuyên người dùng
                              tham khảo linh mục hoặc giáo lý viên.
                            - Ưu tiên câu trả lời ngắn gọn nhưng đầy đủ ý.
                            """),
                    Map.of("role", "user", "content", userInput)
            ));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(API_URL, request, String.class);

            return extractGroqText(response.getBody());

        } catch (Exception e) {
            System.err.println("AI call failed: " + e.getMessage());
            return fallback();
        }
    }

    private String extractGroqText(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(json);
            return node.get("choices").get(0).get("message").get("content").asText();
        } catch (Exception e) {
            return json;
        }
    }

    private String fallback() {
        return """
    Hiện tại tôi chưa thể phản hồi.

    Xin bạn đọc Kinh Thánh hoặc tham khảo ý kiến linh mục để được hướng dẫn.
    """;
    }
}
