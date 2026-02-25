package org.example.catholicsouvenircustomorder.service.imp;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.request.OrderDTO.CreateOrderRequest;
import org.example.catholicsouvenircustomorder.dto.request.OrderDTO.OrderItemRequest;
import org.example.catholicsouvenircustomorder.dto.response.Order.OrderResponseDTO;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.Order;
import org.example.catholicsouvenircustomorder.model.OrderDetail;
import org.example.catholicsouvenircustomorder.model.Product;
import org.example.catholicsouvenircustomorder.repository.OrderRepository;
import org.example.catholicsouvenircustomorder.service.AccountService;
import org.example.catholicsouvenircustomorder.service.OrderService;
import org.example.catholicsouvenircustomorder.service.ProductService;
import org.example.catholicsouvenircustomorder.service.Helper.OrderMapper;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImp implements OrderService {
    private final OrderRepository orderRepository;
    private final AccountService accountService;
    private final ProductService productService;
    private final OrderMapper orderMapper;

    @Override
    public List<OrderResponseDTO> findAll() {
        return orderMapper.toResponseList(orderRepository.findAll());
    }

    @Override
    public List<OrderResponseDTO> findAllByAccountId(UUID accountId) {
        return orderMapper.toResponseList(orderRepository.findByCustomerAccountId(accountId));
    }

    @Override
    public OrderResponseDTO findById(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order không tồn tại"));
        return orderMapper.toResponse(order);
    }

    @Override
    @Retryable(
            value = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(
                    delay = 100,
                    multiplier = 2
            )
    )
    @Transactional
    public OrderResponseDTO create(CreateOrderRequest request) {
        Map<UUID, Product> products = productService.loadAndValidateQuantity(request.getItems());

        BigDecimal total = BigDecimal.ZERO;
        List<OrderDetail> orderDetails = new ArrayList<>();

        for (OrderItemRequest item : request.getItems()) {
            Product product = products.get(item.getProductId());

            OrderDetail orderDetail = orderMapper.toOrderDetail(item);
            orderDetail.setProduct(product);
            orderDetail.setUnitPrice(product.getProductPrice());
            orderDetail.setSubTotal(product.getProductPrice().multiply(BigDecimal.valueOf(item.getQuantity())));

            total = total.add(orderDetail.getSubTotal());
            product.setQuantity(product.getQuantity() - item.getQuantity());

            orderDetails.add(orderDetail);
        }

        Order order = orderMapper.toEntity(request);
        order.setTotal(total);
        order.setStatus("PENDING");
        order.setCreateAt(LocalDateTime.now());
        order.setCustomer(accountService.findAccountById(request.getAccountId()));
        for (OrderDetail detail : orderDetails) {
            detail.setOrder(order);
        }
        order.setOrderDetails(orderDetails);

        orderRepository.save(order);
        return orderMapper.toResponse(order);
    }

    @Override
    public OrderResponseDTO update(UUID orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order không tồn tại"));
        order.setStatus(status);
        orderRepository.save(order);
        return orderMapper.toResponse(order);
    }

    @Override
    public void delete(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order không tồn tại"));
        orderRepository.delete(order);
    }

}