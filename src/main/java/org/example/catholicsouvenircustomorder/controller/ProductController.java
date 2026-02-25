package org.example.catholicsouvenircustomorder.controller;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.response.ProductResponse;
import org.example.catholicsouvenircustomorder.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    @GetMapping()
    public ResponseEntity<BaseResponse> getAll()
    {
        List<ProductResponse> products = productService.findAll();
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách sản phẩm thành công", products));
    }
    @GetMapping("/artisan/{artisanId}")
    public ResponseEntity<BaseResponse> getAllByArtisanId(@PathVariable UUID artisanId){
        List<ProductResponse> products = productService.findAllByArtisanId(artisanId);
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách sản phẩm thành công", products));
    }
    @GetMapping("/{productId}")
    public ResponseEntity<BaseResponse> findById(@PathVariable UUID productId){
        ProductResponse product = productService.findById(productId);
        return ResponseEntity.ok(BaseResponse.success("Lấy thông tin sản phẩm thành công", product));
    }
}
