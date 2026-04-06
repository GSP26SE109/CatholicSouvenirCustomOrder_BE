package org.example.catholicsouvenircustomorder.dto.Event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class ProductDeleteEvent {
    private final UUID productId;
}