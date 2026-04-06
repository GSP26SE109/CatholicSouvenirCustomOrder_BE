package org.example.catholicsouvenircustomorder.dto.Event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.catholicsouvenircustomorder.model.Product;

@Getter
@AllArgsConstructor
public class ProductChangeEvent {
    private final Product product;
}
