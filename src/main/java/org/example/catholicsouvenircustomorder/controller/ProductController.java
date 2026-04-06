package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.ApproveProductRequest;
import org.example.catholicsouvenircustomorder.dto.request.Product.CreateProductRequest;
import org.example.catholicsouvenircustomorder.dto.request.Product.ProductFilterRequest;
import org.example.catholicsouvenircustomorder.dto.request.Product.UpdateProductRequest;
import org.example.catholicsouvenircustomorder.dto.response.Product.ProductResponse;
import org.example.catholicsouvenircustomorder.service.ProductService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("")
    public ResponseEntity<BaseResponse> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "productPrice") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {

        ProductFilterRequest request = new ProductFilterRequest();
        request.setCategory(category);
        request.setTags(tags);
        request.setMinPrice(minPrice);
        request.setMaxPrice(maxPrice);

        Page<ProductResponse> listProducts = productService.filterProducts(
                request, page, size, sortBy, sortDir
        );
        return ResponseEntity.ok(
                BaseResponse.success("Lấy danh sách sản phẩm thành công", listProducts)
        );
    }

    @GetMapping("/approved")
    public ResponseEntity<BaseResponse> getApprovedProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<ProductResponse> products =
                productService.findApprovedProduct(pageable);

        return ResponseEntity.ok(
                BaseResponse.success("Lấy danh sách sản phẩm thành công", products)
        );
    }

    @GetMapping("/artisan/{artisanId}")
    public ResponseEntity<BaseResponse> getMyProducts(
            @RequestParam UUID artisanId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
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

        ProductResponse product = productService.create(request, accountId);

        return ResponseEntity.ok(BaseResponse.success("Tạo sản phẩm thành công", product));
    }

    @PutMapping(path = "/{productId}",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BaseResponse> update(
            @AuthenticationPrincipal UUID artisanId,
            @PathVariable String productId,
            @ModelAttribute UpdateProductRequest dto) {
        ProductResponse product = productService.update(artisanId, UUID.fromString(productId), dto);
        return ResponseEntity.ok(BaseResponse.success("Sửa product thành công", product));
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
