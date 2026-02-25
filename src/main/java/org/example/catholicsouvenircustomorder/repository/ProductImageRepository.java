package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.Product;
import org.example.catholicsouvenircustomorder.model.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {
}
