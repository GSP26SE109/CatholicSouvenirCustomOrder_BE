package org.example.catholicsouvenircustomorder.util;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.Event.ProductChangeEvent;
import org.example.catholicsouvenircustomorder.dto.Event.ProductDeleteEvent;
import org.example.catholicsouvenircustomorder.service.SearchService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ProductIndexListener {

    private final SearchService searchService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ProductChangeEvent event) {
        searchService.index(event.getProduct());
    }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ProductDeleteEvent event) {
        searchService.delete(event.getProductId());
    }
}
