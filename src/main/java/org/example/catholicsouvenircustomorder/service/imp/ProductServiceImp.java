package org.example.catholicsouvenircustomorder.service.imp;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.request.Product.CreateProductRequest;
import org.example.catholicsouvenircustomorder.dto.request.OrderDTO.OrderItemRequest;
import org.example.catholicsouvenircustomorder.dto.request.Product.ProductFilterRequest;
import org.example.catholicsouvenircustomorder.dto.request.Product.UpdateProductImagesRequest;
import org.example.catholicsouvenircustomorder.dto.request.Product.UpdateProductRequest;
import org.example.catholicsouvenircustomorder.dto.response.Product.ProductResponse;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.Account;
import org.example.catholicsouvenircustomorder.model.Product;
import org.example.catholicsouvenircustomorder.model.ProductImage;
import org.example.catholicsouvenircustomorder.repository.AccountRepository;
import org.example.catholicsouvenircustomorder.repository.ArtisanRepository;
import org.example.catholicsouvenircustomorder.repository.ProductRepository;
import org.example.catholicsouvenircustomorder.util.ProductMapper;
import org.example.catholicsouvenircustomorder.service.ProductImageService;
import org.example.catholicsouvenircustomorder.service.ProductService;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Async;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.*;
import org.example.catholicsouvenircustomorder.service.TagService;
import org.example.catholicsouvenircustomorder.specification.ProductSpecification;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
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
    private final AccountRepository accountRepository;
    private final ProductImageService productImageService;
    private final ArtisanRepository artisanRepository;
    private final CategoryRepository categoryRepository;
    private final TagService tagService;

    @Override
    public Page<ProductResponse> findAll(Pageable pageable) {
        Page<Product> productPage = productRepository.findAll(pageable);
        return productPage.map(productMapper::toResponse);
    }
    @Override
    public Page<ProductResponse> findApprovedProduct(Pageable pageable) {
        Page<Product> productPage = productRepository.findProductByStatus("APPROVED",pageable);
        return productPage.map(productMapper::toResponse);
    }

    @Override
    public Page<ProductResponse> search(String keyword,Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return Page.empty(pageable);
        }
        Page<Product> productPage = productRepository.searchWithRanking(keyword,pageable);
        return productPage.map(productMapper::toResponse);
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

        // Validate artisan
        Account artisanAccount = accountRepository.findById(artisanId)
                .orElseThrow(() -> new ResourceNotFoundException("Artisan không tồn tại"));

        if (!"ARTISAN".equals(artisanAccount.getRole().getName())) {
            throw new RuntimeException("Chỉ artisan mới có thể tạo sản phẩm");
        }

        Artisan artisan = artisanRepository.findById(artisanId)
                .orElseThrow(() -> new ResourceNotFoundException("Artisan này không tồn tại"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category không tồn tại"));

        List<Tag> tags = tagService.resolveTags(request.getTags());

        // Create product
        Product product = new Product();
        product.setArtisan(artisan);
        product.setCategory(category);
        product.setTags(tags);
        product.setProductName(request.getProductName());
        product.setProductDescription(request.getProductDescription());
        product.setProductPrice(request.getProductPrice());
        product.setQuantity(request.getQuantity());
        product.setSize(request.getSize());
        product.setStatus("PENDING");
        product.setCreatedAt(LocalDateTime.now());

        Product savedProduct = productRepository.save(product);

        // Images
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            try {
                productImageService.addNewImage(savedProduct.getProductId(), request.getImages());
            } catch (IOException e) {
                throw new RuntimeException("Upload hình ảnh thất bại: " + e.getMessage());
            }
        }
        return productMapper.toResponse(savedProduct);
    }

    @Override
    @Transactional
    public ProductResponse update(UUID artisanId, UUID productId, UpdateProductRequest dto) {

        Product existingProduct = productRepository
                .findProductByProductIdAndArtisan_ArtisanUuid(productId, artisanId)
                .orElseThrow(() -> new ResourceNotFoundException("Product không tồn tại"));

        productMapper.updateProductFromDto(dto, existingProduct);

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category không tồn tại"));
            existingProduct.setCategory(category);
        }
        if (dto.getTags() != null) {
            List<Tag> tags = tagService.resolveTags(dto.getTags());
            existingProduct.setTags(tags);
        }
        if (dto.getDeleteImageIds() != null || dto.getNewImages() != null) {
            UpdateProductImagesRequest imagesRequest = new UpdateProductImagesRequest();
            imagesRequest.setDeleteImageIds(dto.getDeleteImageIds());
            imagesRequest.setNewImages(dto.getNewImages());

            productImageService.updateImages(productId, imagesRequest);
        }

        Product saved = productRepository.save(existingProduct);
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Async
    public void delete(UUID productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product không tồn tại"));
        product.setStatus("DELETED");
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
        productRepository.save(product);
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

    public Page<ProductResponse> filterProducts(
            ProductFilterRequest request,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {

        Specification<Product> spec = (root, query, cb) -> cb.conjunction();

        if (request.getCategory() != null) {
            spec = spec.and(ProductSpecification.hasCategory(request.getCategory()));
        }

        if (request.getTags() != null && !request.getTags().isEmpty()) {
            spec = spec.and(ProductSpecification.hasTags(request.getTags()));
        }

        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            spec = spec.and(ProductSpecification.priceBetween(
                    request.getMinPrice(),
                    request.getMaxPrice()
            ));
        }

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> productPage = productRepository.findAll(spec, pageable);

        return productPage.map(productMapper::toResponse);
    }
}
