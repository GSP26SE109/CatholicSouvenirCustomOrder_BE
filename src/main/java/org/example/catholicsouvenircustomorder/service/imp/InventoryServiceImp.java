package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.Order;
import org.example.catholicsouvenircustomorder.model.OrderDetail;
import org.example.catholicsouvenircustomorder.model.OrderGroup;
import org.example.catholicsouvenircustomorder.model.Product;
import org.example.catholicsouvenircustomorder.repository.OrderRepository;
import org.example.catholicsouvenircustomorder.repository.ProductRepository;
import org.example.catholicsouvenircustomorder.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImp implements InventoryService {
    
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    
    @Override
    @Transactional
    public void confirmReservations(OrderGroup orderGroup) {
        log.info("Confirming inventory reservations for order group: {}", orderGroup.getGroupId());
        
        int totalConfirmed = 0;
        for (Order order : orderGroup.getOrders()) {
            for (OrderDetail detail : order.getOrderDetails()) {
                Product product = detail.getProduct();
                int quantity = detail.getQuantity();
                
                log.info("Confirming {} units of product {} (current: qty={}, reserved={})",
                    quantity, product.getProductId(), product.getQuantity(), product.getReservedQuantity());
                
                product.confirmReservation(quantity);
                productRepository.save(product);
                
                log.info("✅ Confirmed {} units of product {} (new: qty={}, reserved={})",
                    quantity, product.getProductId(), product.getQuantity(), product.getReservedQuantity());
                
                totalConfirmed += quantity;
            }
        }
        
        log.info("✅ Confirmed total {} units across {} orders", 
            totalConfirmed, orderGroup.getOrders().size());
    }
    
    @Override
    @Transactional
    public void releaseReservations(OrderGroup orderGroup) {
        log.info("Releasing inventory reservations for order group: {}", orderGroup.getGroupId());
        
        int totalReleased = 0;
        for (Order order : orderGroup.getOrders()) {
            for (OrderDetail detail : order.getOrderDetails()) {
                Product product = detail.getProduct();
                int quantity = detail.getQuantity();
                
                log.info("Releasing {} units of product {} (current: qty={}, reserved={})",
                    quantity, product.getProductId(), product.getQuantity(), product.getReservedQuantity());
                
                product.releaseReservation(quantity);
                productRepository.save(product);
                
                log.info("✅ Released {} units of product {} (new: qty={}, reserved={})",
                    quantity, product.getProductId(), product.getQuantity(), product.getReservedQuantity());
                
                totalReleased += quantity;
            }
        }
        
        log.info("✅ Released total {} units across {} orders", 
            totalReleased, orderGroup.getOrders().size());
    }
    
    @Override
    @Transactional
    public void releaseReservationsForOrder(UUID orderId) {
        log.info("Releasing inventory reservations for order: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        
        int totalReleased = 0;
        for (OrderDetail detail : order.getOrderDetails()) {
            Product product = detail.getProduct();
            int quantity = detail.getQuantity();
            
            product.releaseReservation(quantity);
            productRepository.save(product);
            
            totalReleased += quantity;
        }
        
        log.info("✅ Released total {} units for order {}", totalReleased, orderId);
    }
}
