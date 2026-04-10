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
                            "You are a Catholic assistant. Answer based on the Bible, " +
                                    "Catechism of the Catholic Church, and Church tradition. " +
                                    "Be respectful and pastoral."),
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
}
