package org.example.catholicsouvenircustomorder.dto.request;

import lombok.Data;

@Data
public class SearchRequest {
    private String keyword;
    private String category;
    private Double minPrice;
    private Double maxPrice;
}