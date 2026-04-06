package org.example.catholicsouvenircustomorder.controller;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.service.SearchService;
import org.example.catholicsouvenircustomorder.util.ProductDocument;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.example.catholicsouvenircustomorder.dto.request.SearchRequest;
import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {
    private final SearchService searchService;

    @GetMapping("/suggest")
    public List<String> suggest(@RequestParam String keyword) {
        return searchService.suggest(keyword);
    }

    @PostMapping()
    public ResponseEntity<BaseResponse> search(@RequestBody SearchRequest request) {
        List<ProductDocument> results = searchService.search(request);
        return ResponseEntity.ok(BaseResponse.success("Tìm thành công", results));
    }
}
