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
        if (!isCatholicQuestion(userInput)) {
            return "Xin lỗi, tôi chỉ hỗ trợ các câu hỏi liên quan đến Công giáo.";
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(api_key);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("model", "llama-3.3-70b-versatile");
            body.put("messages", List.of(
                    Map.of("role", "system", "content",
                            """
                            Bạn là trợ lý AI Công giáo chỉ chuyên trả lời các câu hỏi liên quan đến Đức tin Công giáo.
                    
                            CHỈ được trả lời các chủ đề sau:
                            - Kinh Thánh
                            - Giáo lý Hội Thánh Công giáo
                            - Bí tích
                            - Thánh lễ
                            - Cầu nguyện
                            - Các Thánh
                            - Luân lý Công giáo
                            - Phụng vụ
                            - Đức Mẹ
                            - Lịch sử Hội Thánh
                            - Đời sống thiêng liêng Kitô giáo
                    
                            Nếu người dùng hỏi ngoài phạm vi trên như:
                            - lập trình
                            - toán học
                            - bóng đá
                            - chính trị
                            - công nghệ
                            - giải trí
                            - chuyện đời thường
                    
                            THÌ KHÔNG TRẢ LỜI nội dung đó.
                    
                            Hãy đáp:
                            "Xin lỗi, tôi chỉ hỗ trợ các câu hỏi trong bối cảnh Công giáo như Kinh Thánh, Giáo lý, cầu nguyện và đời sống đức tin."
                    
                            Luôn trả lời bằng tiếng Việt, ngắn gọn, hiền hòa, rõ ràng.
                            """
                    ),
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
        I'm currently unable to respond.

        Please reflect on Scripture or consult a priest for guidance.
        """;
    }
    private boolean isCatholicQuestion(String text) {
        String input = text.toLowerCase();

        return input.contains("chúa")
                || input.contains("thiên chúa")
                || input.contains("đức chúa")
                || input.contains("giêsu")
                || input.contains("jesus")
                || input.contains("kitô")
                || input.contains("christ")
                || input.contains("đức kitô")
                || input.contains("thánh thần")
                || input.contains("chúa thánh thần")
                || input.contains("ba ngôi")
                || input.contains("ba ngôi thiên chúa")

                // Catholic identity
                || input.contains("công giáo")
                || input.contains("catholic")
                || input.contains("giáo hội")
                || input.contains("hội thánh")
                || input.contains("roma")
                || input.contains("vatican")
                || input.contains("toà thánh")

                // Bible
                || input.contains("kinh thánh")
                || input.contains("thánh kinh")
                || input.contains("phúc âm")
                || input.contains("tin mừng")
                || input.contains("cựu ước")
                || input.contains("tân ước")
                || input.contains("thánh vịnh")
                || input.contains("sáng thế")
                || input.contains("mátthêu")
                || input.contains("mác cô")
                || input.contains("luca")
                || input.contains("gioan")
                || input.contains("paolô")

                // Mary & Saints
                || input.contains("đức mẹ")
                || input.contains("maria")
                || input.contains("mẹ maria")
                || input.contains("mẹ thiên chúa")
                || input.contains("thánh giuse")
                || input.contains("thánh phêrô")
                || input.contains("thánh phaolô")
                || input.contains("các thánh")
                || input.contains("thánh")

                // Sacraments
                || input.contains("bí tích")
                || input.contains("rửa tội")
                || input.contains("thêm sức")
                || input.contains("thánh thể")
                || input.contains("mình thánh")
                || input.contains("xưng tội")
                || input.contains("giải tội")
                || input.contains("hòa giải")
                || input.contains("truyền chức")
                || input.contains("hôn phối")
                || input.contains("xức dầu")

                // Worship
                || input.contains("thánh lễ")
                || input.contains("lễ misa")
                || input.contains("misa")
                || input.contains("phụng vụ")
                || input.contains("nhà thờ")
                || input.contains("giáo xứ")
                || input.contains("giáo phận")
                || input.contains("giám mục")
                || input.contains("linh mục")
                || input.contains("phó tế")
                || input.contains("tu sĩ")
                || input.contains("nữ tu")
                || input.contains("cha xứ")

                // Prayer
                || input.contains("cầu nguyện")
                || input.contains("kinh nguyện")
                || input.contains("kinh lạy cha")
                || input.contains("kinh kính mừng")
                || input.contains("kinh sáng danh")
                || input.contains("mân côi")
                || input.contains("lần chuỗi")
                || input.contains("chầu thánh thể")

                // Doctrine / morality
                || input.contains("giáo lý")
                || input.contains("tín điều")
                || input.contains("đức tin")
                || input.contains("ơn cứu độ")
                || input.contains("thiên đàng")
                || input.contains("luyện ngục")
                || input.contains("hoả ngục")
                || input.contains("tội")
                || input.contains("tội trọng")
                || input.contains("tội nhẹ")
                || input.contains("ăn năn")
                || input.contains("sám hối")

                // Catholic dates
                || input.contains("mùa vọng")
                || input.contains("mùa chay")
                || input.contains("phục sinh")
                || input.contains("giáng sinh")
                || input.contains("lễ tro")
                || input.contains("tuần thánh")
                || input.contains("chúa nhật")

                // Pope
                || input.contains("giáo hoàng")
                || input.contains("đức giáo hoàng")
                || input.contains("pope");
    }
}
