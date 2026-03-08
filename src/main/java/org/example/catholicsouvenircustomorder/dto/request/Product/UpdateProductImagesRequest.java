package org.example.catholicsouvenircustomorder.dto.request.Product;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Data
public class UpdateProductImagesRequest {

    private List<UUID> deleteImageIds;

    private List<MultipartFile> newImages;
}