package org.example.catholicsouvenircustomorder.service.imp;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.request.CreateProductRequest;
import org.example.catholicsouvenircustomorder.dto.request.OrderDTO.OrderItemRequest;
import org.example.catholicsouvenircustomorder.dto.request.ProductCreateDTO;
import org.example.catholicsouvenircustomorder.dto.response.Product.ProductResponse;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.Product;
import org.example.catholicsouvenircustomorder.model.ProductImage;
import org.example.catholicsouvenircustomorder.repository.ProductImageRepository;
import org.example.catholicsouvenircustomorder.repository.ProductRepository;
import org.example.catholicsouvenircustomorder.service.Helper.ProductMapper;
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
    public ProductResponse create(CreateProductRequest request) {

        Product product = new Product();
        product.setArtisanId(request.getArtisanId());
        product.setProductName(request.getProductName());
        product.setProductDescription(request.getProductDescription());
        product.setProductPrice(request.getProductPrice());
        product.setQuantity(request.getQuantity());
        product.setMaterial(request.getMaterial());
        product.setSize(request.getSize());
        product.setStatus("PENDING");
        product.setCreatedAt(LocalDateTime.now());
        productRepository.save(product);

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            try {

                Map uploadResult = cloudinary.uploader().upload(
                        request.getImage().getBytes(),
                        ObjectUtils.emptyMap()
                );

                String imageUrl = uploadResult.get("secure_url").toString();
                String publicId = uploadResult.get("public_id").toString();

                ProductImage productImage = new ProductImage();
                productImage.setImage_url(imageUrl);
                productImage.setPublicId(publicId);
                productImage.setProduct(product);

                productImageRepository.save(productImage);

            } catch (IOException e) {
                throw new RuntimeException("Upload hình ảnh thất bại");
            }
        }

        return productMapper.toResponse(product);
    }

    @Override
    public ProductResponse update(UUID productId, ProductCreateDTO dto) {

        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product không tồn tại"));

        productMapper.updateProductFromDto(dto, existingProduct);

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
    public ProductResponse ApproveProduct(UUID productId, String status) {

        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product không tồn tại"));

        existingProduct.setStatus(status);

        Product saved = productRepository.save(existingProduct);

        return productMapper.toResponse(saved);
    }
}
