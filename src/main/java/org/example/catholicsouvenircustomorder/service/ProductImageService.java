package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.Product.UpdateProductImagesRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface ProductImageService {
    void updateImages(UUID productId, UpdateProductImagesRequest request);
    void addNewImage(UUID productId, List<MultipartFile> images) throws IOException;
}
