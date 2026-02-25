package org.example.catholicsouvenircustomorder.service.imp;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.request.OrderDTO.OrderItemRequest;
import org.example.catholicsouvenircustomorder.dto.request.ProductCreateDTO;
import org.example.catholicsouvenircustomorder.model.Product;
import org.example.catholicsouvenircustomorder.repository.ProductRepository;
import org.example.catholicsouvenircustomorder.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImp implements ProductService {
    @Autowired
    private ProductRepository productRepository;
    private final LocalDateTime currentTime = LocalDateTime.now();
    @Override
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    public List<Product> findAllByArtisanId(UUID artisanId) {
        return productRepository.findByArtisanId(artisanId);
    }

    @Override
    public Product findById(UUID id) {
        return productRepository.findById(id).orElse(null);
    }

    @Override
    public Product create(ProductCreateDTO product) {
        Product newProduct = new Product();
        newProduct.setArtisanId(product.getArtisanId());
        newProduct.setProductDescription(product.getProductDescription());
        newProduct.setProductName(product.getProductName());
        newProduct.setProductPrice(product.getProductPrice());
        newProduct.setQuantity(product.getQuantity());
        newProduct.setStatus(product.isStatus());
        newProduct.setProductImages(product.getProductImages());
        newProduct.setCreatedAt(currentTime);
        return productRepository.save(newProduct);
    }

    @Override
    public Product update(UUID productId, ProductCreateDTO product) {
        Product updatedProduct = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));
        updatedProduct.setArtisanId(product.getArtisanId());
        updatedProduct.setProductDescription(product.getProductDescription());
        updatedProduct.setProductName(product.getProductName());
        updatedProduct.setProductPrice(product.getProductPrice());
        updatedProduct.setQuantity(product.getQuantity());
        updatedProduct.setStatus(product.isStatus());
        updatedProduct.setProductImages(product.getProductImages());
        return productRepository.save(updatedProduct);
    }

    @Override
    public void delete(UUID productId) {
        Product product = productRepository.findById(productId).
                orElseThrow(() -> new EntityNotFoundException("Product not found"));
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
                .collect(Collectors.toMap(Product::getProductUuid, p -> p));

        for (OrderItemRequest item : items) {
            Product product = products.get(item.getProductId());
            if (product.getQuantity() < item.getQuantity()) {
                throw new RuntimeException("Not enough quantity");
            }
        }
        return products;
    }
}
