package org.example.catholicsouvenircustomorder.service.imp;

import lombok.AllArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.request.SearchRequest;
import org.example.catholicsouvenircustomorder.model.Product;
import org.example.catholicsouvenircustomorder.repository.ProductRepository;
import org.example.catholicsouvenircustomorder.service.SearchService;
import org.example.catholicsouvenircustomorder.util.ProductDocument;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class SearchServiceImp implements SearchService {
    private final ElasticsearchOperations operations;
    private final ProductRepository productRepository;
    public List<ProductDocument> search(SearchRequest request) {

        Query query = NativeQuery.builder()
                .withQuery(q -> q
                        .multiMatch(m -> m
                                .query(request.getKeyword())
                                .fields(
                                        "name^3",          // highest priority
                                        "artisanName^2",
                                        "tags^2",
                                        "category",
                                        "description"
                                )
                        )
                )
                .build();

        return operations.search(query, ProductDocument.class)
                .stream()
                .map(SearchHit::getContent)
                .toList();
    }
    public List<String> suggest(String keyword) {

        Query query = NativeQuery.builder()
                .withQuery(q -> q
                        .match(m -> m
                                .field("name")
                                .query(keyword)
                        )
                )
                .build();

        return operations.search(query, ProductDocument.class)
                .stream()
                .map(hit -> hit.getContent().getName())
                .distinct()
                .toList();
    }
    public void index(Product product) {
        ProductDocument doc = mapToDocument(product);
        operations.save(doc);
    }
    public void delete(UUID productId) {
        operations.delete(productId.toString(), ProductDocument.class);
    }
    private ProductDocument mapToDocument(Product product) {
        return ProductDocument.builder()
                .id(product.getProductId().toString())
                .name(product.getProductName())
                .description(product.getProductDescription())
                .artisanId(product.getArtisan().getArtisanUuid().toString())
                .artisanName(product.getArtisan().getArtisanName())
               // .category(product.getCategory().getName())
                //.tags(product.getTags().stream().map(Tag::getName).toList())
                .build();
    }
}
