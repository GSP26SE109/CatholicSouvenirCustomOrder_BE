package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.request.OrderDTO.OrderItemRequest;
import org.example.catholicsouvenircustomorder.dto.request.ProductCreateDTO;
import org.example.catholicsouvenircustomorder.dto.response.ProductResponse;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.Product;
import org.example.catholicsouvenircustomorder.repository.ProductRepository;
import org.example.catholicsouvenircustomorder.service.Helper.ProductMapper;
import org.example.catholicsouvenircustomorder.service.ProductService;
import org.springframework.stereotype.Service;
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
    @Override
    public List<ProductResponse> findAll() {
         List<Product> productList= productRepository.findAll();
        return productMapper.toResponseList(productList);
    }

    @Override
    public List<ProductResponse> findAllByArtisanId(UUID artisanId) {
        List<Product> product= productRepository.findByArtisanId(artisanId);
        return productMapper.toResponseList(product);
    }

    @Override
    public ProductResponse findById(UUID id) {
        Product product= productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return productMapper.toResponse(product);
    }

    @Override
    public ProductResponse create(ProductCreateDTO product) {

        Product newProduct = productMapper.toEntity(product);

        newProduct.setCreatedAt(LocalDateTime.now());

        Product saved = productRepository.save(newProduct);

        return productMapper.toResponse(saved);
    }
    @Override
    public ProductResponse update(UUID productId, ProductCreateDTO dto) {

        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        productMapper.updateProductFromDto(dto, existingProduct);

        Product saved = productRepository.save(existingProduct);

        return productMapper.toResponse(saved);
    }

    @Override
    public void delete(UUID productId) {
        Product product = productRepository.findById(productId).
                orElseThrow(() -> new ResourceNotFoundException("Product not found"));
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
                throw new RuntimeException("Not enough quantity");
            }
        }
        return products;
    }
}
