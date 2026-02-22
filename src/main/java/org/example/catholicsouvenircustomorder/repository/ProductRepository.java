package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product,Integer> {
    List<Product> findByArtisanId(UUID artisanId);
}
