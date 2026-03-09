package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.service.AIImageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIImageServiceImp implements AIImageService {

    @Value("${ai.image.provider:huggingface}")
    private String imageProvider; // huggingface, stable-diffusion, openai

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    @Value("${huggingface.api.key:}")
    private String huggingfaceApiKey;

    @Value("${stable-diffusion.api.url:http://localhost:7860}")
    private String stableDiffusionUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String generateImage(String prompt) {
        try {
            String enhancedPrompt = enhancePromptForReligiousArt(prompt);
            
            switch (imageProvider.toLowerCase()) {
                case "huggingface":
                    return generateWithHuggingFace(enhancedPrompt);
                case "stable-diffusion":
                    return generateWithStableDiffusion(enhancedPrompt);
                case "openai":
                    return generateWithOpenAI(enhancedPrompt);
                default:
                    log.warn("Unknown image provider: {}, falling back to Hugging Face", imageProvider);
                    return generateWithHuggingFace(enhancedPrompt);
            }
        } catch (Exception e) {
            log.error("Error generating AI image: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Generate image using Hugging Face (FREE)
     * Model: stabilityai/stable-diffusion-2-1
     */
    private String generateWithHuggingFace(String prompt) {
        if (huggingfaceApiKey == null || huggingfaceApiKey.isEmpty()) {
            log.warn("Hugging Face API key not configured");
            return null;
        }

        try {
            String apiUrl = "https://api-inference.huggingface.co/models/stabilityai/stable-diffusion-2-1";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + huggingfaceApiKey);

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
                // Convert byte[] to base64 string
                String base64Image = Base64.getEncoder().encodeToString(response.getBody());
                return "data:image/png;base64," + base64Image;
            }

            log.error("Failed to generate image with Hugging Face");
            return null;

        } catch (Exception e) {
            log.error("Error calling Hugging Face API: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Generate image using local Stable Diffusion (FREE - Self-hosted)
     * Requires Stable Diffusion WebUI running with --api flag
     */
    private String generateWithStableDiffusion(String prompt) {
        try {
            String apiUrl = stableDiffusionUrl + "/sdapi/v1/txt2img";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("prompt", prompt);
            requestBody.put("negative_prompt", "low quality, blurry, distorted, modern, cartoon");
            requestBody.put("steps", 20);
            requestBody.put("width", 512);
            requestBody.put("height", 512);
            requestBody.put("cfg_scale", 7);
            requestBody.put("sampler_name", "DPM++ 2M Karras");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<String> images = (List<String>) response.getBody().get("images");
                if (images != null && !images.isEmpty()) {
                    return "data:image/png;base64," + images.get(0);
                }
            }

            log.error("Failed to generate image with Stable Diffusion");
            return null;

        } catch (Exception e) {
            log.error("Error calling Stable Diffusion API: {}", e.getMessage());
            log.info("Make sure Stable Diffusion WebUI is running with --api flag");
            return null;
        }
    }

    /**
     * Generate image using OpenAI DALL-E (PAID)
     */
    private String generateWithOpenAI(String prompt) {
        if (openaiApiKey == null || openaiApiKey.isEmpty()) {
            log.warn("OpenAI API key not configured");
            return null;
        }

        try {
            String apiUrl = "https://api.openai.com/v1/images/generations";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "dall-e-3");
            requestBody.put("prompt", prompt);
            requestBody.put("n", 1);
            requestBody.put("size", "1024x1024");
            requestBody.put("quality", "standard");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, String>> data = (List<Map<String, String>>) response.getBody().get("data");
                if (data != null && !data.isEmpty()) {
                    return data.get(0).get("url");
                }
            }

            log.error("Failed to generate image with OpenAI");
            return null;

        } catch (Exception e) {
            log.error("Error calling OpenAI API: {}", e.getMessage());
            return null;
        }
    }

    private String enhancePromptForReligiousArt(String originalPrompt) {
        return "Catholic religious art, traditional style, peaceful and reverent: " + 
               originalPrompt + 
               ". High quality craftsmanship, detailed, beautiful, sacred art.";
    }
}