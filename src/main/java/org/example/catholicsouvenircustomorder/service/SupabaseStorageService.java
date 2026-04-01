package org.example.catholicsouvenircustomorder.service;

public interface SupabaseStorageService {
    /**
     * Upload image to Supabase Storage and return public URL
     */
    String uploadImage(byte[] imageBytes, String fileName);
    
    /**
     * Delete image from Supabase Storage
     */
    void deleteImage(String filePath);
}
