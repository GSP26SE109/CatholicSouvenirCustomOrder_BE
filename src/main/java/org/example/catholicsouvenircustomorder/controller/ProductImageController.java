package org.example.catholicsouvenircustomorder.controller;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.Product.UpdateProductImagesRequest;
import org.example.catholicsouvenircustomorder.model.ProductImage;
import org.example.catholicsouvenircustomorder.service.ProductImageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/product-image")
@RequiredArgsConstructor
public class ProductImageController {
    private final ProductImageService productImageService;
    @PostMapping("/{productId}")
    public ResponseEntity<BaseResponse> addProductImage(
            @PathVariable UUID productId,
            @ModelAttribute List<MultipartFile> images) throws IOException {

        productImageService.addNewImage(productId, images);

        return ResponseEntity.ok(BaseResponse.success("Upload hình ảnh thành công"));
    }
    @PutMapping("/{productId}")
    public ResponseEntity<BaseResponse> updateProductImage(
            @PathVariable UUID productId,
            @ModelAttribute UpdateProductImagesRequest request) throws IOException {

        productImageService.updateImages(productId, request);

        return ResponseEntity.ok(BaseResponse.success("Update hình ảnh thành công"));
    }
}
