package org.example.catholicsouvenircustomorder.controller;

import org.example.catholicsouvenircustomorder.model.Product;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @PostMapping
    public String addProduct(@RequestBody Product product) {
        return null;
    }
}
