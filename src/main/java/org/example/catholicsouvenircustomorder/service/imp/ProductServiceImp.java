package org.example.catholicsouvenircustomorder.service.imp;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.request.Product.CreateProductRequest;
import org.example.catholicsouvenircustomorder.dto.request.OrderDTO.OrderItemRequest;
import org.example.catholicsouvenircustomorder.dto.request.Product.UpdateProductRequest;
import org.example.catholicsouvenircustomorder.dto.response.Product.ProductResponse;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.Account;
import org.example.catholicsouvenircustomorder.model.Product;
import org.example.catholicsouvenircustomorder.model.ProductImage;
import org.example.catholicsouvenircustomorder.repository.AccountRepository;
import org.example.catholicsouvenircustomorder.repository.ArtisanRepository;
import org.example.catholicsouvenircustomorder.repository.ProductImageRepository;
import org.example.catholicsouvenircustomorder.repository.ProductRepository;
import org.example.catholicsouvenircustomorder.Utils.Helper.ProductMapper;
import org.example.catholicsouvenircustomorder.service.ProductImageService;
import org.example.catholicsouvenircustomorder.service.ProductService;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImp implements ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final Cloudinary cloudinary;
    private final ProductImageRepository productImageRepository;
    private final AccountRepository accountRepository;
    private final ProductImageService productImageService;
    private final ArtisanRepository artisanRepository;

    @Override
    public List<ProductResponse> findAll() {
        List<Product> productList = productRepository.findAll();
        return productMapper.toResponseList(productList);
    }

    @Override
    public Page<ProductResponse> findAllByArtisanId(
            UUID artisanId,
            String status,
            Pageable pageable
    ) {
        return productRepository
                .findByArtisanIdWithOptionalStatus(artisanId, status, pageable)
                .map(productMapper::toResponse);
    }

    @Override
    public ProductResponse findById(UUID id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse create(CreateProductRequest request, UUID artisanId) {
        // Validate artisan exists và có role ARTISAN
        Account artisan = accountRepository.findById(artisanId)
                .orElseThrow(() -> new ResourceNotFoundException("Artisan không tồn tại"));
        
        if (!"ARTISAN".equals(artisan.getRole().getName())) {
            throw new RuntimeException("Chỉ artisan mới có thể tạo sản phẩm");
        }

        // Tạo product
        Product product = new Product();
        product.setArtisan(artisanRepository.findById(artisanId).orElseThrow(() -> new ResourceNotFoundException("Artisan này không tồn tại")));
        product.setProductName(request.getProductName());
        product.setProductDescription(request.getProductDescription());
        product.setProductPrice(request.getProductPrice());
        product.setQuantity(request.getQuantity());
        product.setMaterial(request.getMaterial());
        product.setSize(request.getSize());
        product.setStatus("PENDING");
        product.setCreatedAt(LocalDateTime.now());
        
        Product savedProduct = productRepository.save(product);

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            try {
                productImageService.addNewImage(product.getProductId(), request.getImages());
            } catch (IOException e) {
                throw new RuntimeException("Upload hình ảnh thất bại: " + e.getMessage());
            }
        }

        return productMapper.toResponse(savedProduct);
    }

    @Override
    public ProductResponse update(UUID productId, UpdateProductRequest dto) {

        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product không tồn tại"));

        productMapper.updateProductFromDto(dto, existingProduct);
        productImageService.updateImages(productId,dto.getImages());
        Product saved = productRepository.save(existingProduct);

        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(UUID productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product không tồn tại"));

        for (ProductImage image : product.getImages()) {
            try {
                cloudinary.uploader().destroy(
                        image.getPublicId(),
                        ObjectUtils.emptyMap()
                );
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete image from Cloudinary");
            }
        }
        productRepository.delete(product);
    }

    @Override
    public Map<UUID, Product> loadAndValidateQuantity(List<OrderItemRequest> items) {

        Map<UUID, Product> products = productRepository
                .findAllById(
                        items.stream()
                                .map(OrderItemRequest::getProductId)
                                .toList()
                )
                .stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));

        for (OrderItemRequest item : items) {
            Product product = products.get(item.getProductId());
            if (product.getQuantity() < item.getQuantity()) {
                throw new RuntimeException("Không đủ số lượng");
            }
        }
        return products;
    }

    @Override
    public ProductResponse ApproveProduct(UUID productId, org.example.catholicsouvenircustomorder.dto.request.ApproveProductRequest request) {

        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product không tồn tại"));

        // Validate: nếu REJECTED thì phải có lý do
        if ("REJECTED".equals(request.getStatus()) && 
            (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty())) {
            throw new RuntimeException("Lý do từ chối không được để trống khi từ chối sản phẩm");
        }

        existingProduct.setStatus(request.getStatus());
        
        // Lưu lý do từ chối nếu có (cần thêm field rejectionReason vào Product model)
        // existingProduct.setRejectionReason(request.getRejectionReason());

        Product saved = productRepository.save(existingProduct);

        return productMapper.toResponse(saved);
    }
}
