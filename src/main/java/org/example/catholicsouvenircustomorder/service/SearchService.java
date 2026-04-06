package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.SearchRequest;
import org.example.catholicsouvenircustomorder.model.Product;
import org.example.catholicsouvenircustomorder.util.ProductDocument;

import java.util.List;
import java.util.UUID;

public interface SearchService {
    List<ProductDocument> search(SearchRequest request);
    List<String> suggest(String keyword);
    void index(Product product);
    void delete(UUID productId);
}
