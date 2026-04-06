package org.example.catholicsouvenircustomorder.service.imp;

import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.service.SupabaseStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
@Slf4j
public class SupabaseStorageServiceImp implements SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${supabase.storage.bucket:ai-images}")
    private String bucketName;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String uploadImage(byte[] imageBytes, String fileName) {
        try {
            String uniqueFileName = UUID.randomUUID() + "_" + fileName + ".png";
            String uploadUrl = String.format("%s/storage/v1/object/%s/%s", 
                supabaseUrl, bucketName, uniqueFileName);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.set("Authorization", "Bearer " + supabaseKey);
            headers.set("apikey", supabaseKey);
            headers.set("cache-control", "3600");
            headers.set("x-upsert", "false");

            HttpEntity<byte[]> entity = new HttpEntity<>(imageBytes, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                uploadUrl,
                HttpMethod.POST,
                entity,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK || 
                response.getStatusCode() == HttpStatus.CREATED ||
                response.getStatusCode() == HttpStatus.NO_CONTENT) {
                String publicUrl = String.format("%s/storage/v1/object/public/%s/%s",
                    supabaseUrl, bucketName, uniqueFileName);
                
                log.info("Successfully uploaded image to Supabase: {}", publicUrl);
                return publicUrl;
            } else {
                log.error("Failed to upload to Supabase. Status: {}, Body: {}", 
                    response.getStatusCode(), response.getBody());
                return null;
            }

        } catch (Exception e) {
            log.error("Error uploading to Supabase Storage: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void deleteImage(String filePath) {
        try {
            // Extract filename from URL
            String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
            
            String deleteUrl = String.format("%s/storage/v1/object/%s/%s",
                supabaseUrl, bucketName, fileName);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey);
            headers.set("apikey", supabaseKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.exchange(
                deleteUrl,
                HttpMethod.DELETE,
                entity,
                String.class
            );

            log.info("Deleted image from Supabase: {}", fileName);

        } catch (Exception e) {
            log.error("Error deleting from Supabase Storage: {}", e.getMessage(), e);
        }
    }
}
