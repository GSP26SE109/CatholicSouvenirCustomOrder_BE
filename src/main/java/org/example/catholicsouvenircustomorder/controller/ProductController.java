package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.ApproveProductRequest;
import org.example.catholicsouvenircustomorder.dto.request.ProductCreateDTO;
import org.example.catholicsouvenircustomorder.dto.request.Product.CreateProductRequest;
import org.example.catholicsouvenircustomorder.dto.request.Product.UpdateProductRequest;
import org.example.catholicsouvenircustomorder.dto.response.Product.ProductResponse;
import org.example.catholicsouvenircustomorder.service.ProductService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping()
    public ResponseEntity<BaseResponse> getAll() {
        List<ProductResponse> products = productService.findAll();
        return ResponseEntity.ok(BaseResponse.success("Lấy danh sách sản phẩm thành công", products));
    }

    @GetMapping("/artisan/{artisanId}")
    public ResponseEntity<BaseResponse> getMyProducts(
            @AuthenticationPrincipal UUID artisanId,
            @RequestParam(required = false) String status,
            @ParameterObject
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<ProductResponse> products =
                productService.findAllByArtisanId(artisanId, status, pageable);

        return ResponseEntity.ok(
                BaseResponse.success("Lấy danh sách sản phẩm thành công", products)
        );
    }

    @GetMapping("/{productId}")
    public ResponseEntity<BaseResponse> findById(@PathVariable UUID productId) {
        ProductResponse product = productService.findById(productId);
        return ResponseEntity.ok(BaseResponse.success("Lấy thông tin sản phẩm thành công", product));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse> create(
            @ModelAttribute CreateProductRequest request, @AuthenticationPrincipal UUID accountId) {

        ProductResponse product=productService.create(request, accountId);

        return ResponseEntity.ok(BaseResponse.success("Product created",product));
    }
    @PutMapping("/{productId}")
    public ResponseEntity<BaseResponse> update(@PathVariable String productId, @RequestBody UpdateProductRequest dto) {
        ProductResponse product = productService.update(UUID.fromString(productId), dto);
        return ResponseEntity.ok(BaseResponse.success("Sửa product thành công",product));
    }
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable String productId) {
        productService.delete(UUID.fromString(productId));
        return ResponseEntity.noContent().build();
    }
    @PutMapping("/admin/{productId}")
    public ResponseEntity<BaseResponse> approveProduct(
            @PathVariable UUID productId, 
            @Valid @RequestBody ApproveProductRequest request) {
        ProductResponse product = productService.ApproveProduct(productId, request);
        
        String message = "APPROVED".equals(request.getStatus()) 
            ? "Phê duyệt sản phẩm thành công" 
            : "Từ chối sản phẩm thành công";
            
        return ResponseEntity.ok(BaseResponse.success(message, product));
    }
}
