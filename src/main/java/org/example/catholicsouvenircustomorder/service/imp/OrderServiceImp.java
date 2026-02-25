package org.example.catholicsouvenircustomorder.service.imp;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.request.OrderDTO.CreateOrderRequest;
import org.example.catholicsouvenircustomorder.dto.request.OrderDTO.OrderItemRequest;
import org.example.catholicsouvenircustomorder.dto.response.Order.OrderDetailResponseDTO;
import org.example.catholicsouvenircustomorder.dto.response.Order.OrderResponseDTO;
import org.example.catholicsouvenircustomorder.model.Order;
import org.example.catholicsouvenircustomorder.model.OrderDetail;
import org.example.catholicsouvenircustomorder.model.Product;
import org.example.catholicsouvenircustomorder.repository.OrderRepository;
import org.example.catholicsouvenircustomorder.service.AccountService;
import org.example.catholicsouvenircustomorder.service.OrderService;

import org.example.catholicsouvenircustomorder.service.ProductService;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
    private final LocalDateTime currentTime = LocalDateTime.now();
    private ProductService productService;

    @Override
    public List<OrderResponseDTO> findAll() {
        List<Order> list = orderRepository.findAll();
        List<OrderResponseDTO> orderResponseDTOS = new ArrayList<>();
        for (Order order : list) {
            orderResponseDTOS.add(mapping(order));
        }
        return orderResponseDTOS;
    }

    @Override
    public List<OrderResponseDTO> findAllByAccountId(UUID accountId) {
        List<Order> list = orderRepository.findByCustomerAccountId(accountId);
        List<OrderResponseDTO> orderResponseDTOS = new ArrayList<>();
        for (Order order : list) {
            orderResponseDTOS.add(mapping(order));
        }
        return orderResponseDTOS;
    }

    @Override
    public OrderResponseDTO findById(UUID id) {
        Order order = orderRepository.findById(id).orElse(null);
        return mapping(order);
    }

    @Override
    @Transactional
    public OrderResponseDTO create(CreateOrderRequest request) {
        Map<UUID, Product> products = productService.loadAndValidateQuantity(request.getItems());
        BigDecimal total=BigDecimal.ZERO;
        List<OrderDetail> orderDetails = new ArrayList<>();
        for (OrderItemRequest item : request.getItems()) {
            Product product = products.get(item.getProductId());

            OrderDetail orderDetail = new OrderDetail();

            total =total.add(product.getProductPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            product.setQuantity(product.getQuantity() - item.getQuantity());

            orderDetail.setProduct(product);
            orderDetail.setQuantity(item.getQuantity());
            orderDetail.setUnitPrice(product.getProductPrice());
            orderDetail.setSubTotal(product.getProductPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            orderDetails.add(orderDetail);
        }
        Order order = new Order();
        order.setOrderDate(request.getOrderDate());
        order.setTotal(total);
        order.setStatus("PENDING");
        order.setPaymentMethod(request.getPaymentMethod());
        order.setCreateAt(currentTime);
        order.setCustomer(accountService.findAccountById(request.getAccountId()));
        order.setOrderDetails(orderDetails);
        orderRepository.save(order);
        return mapping(order);
    }

    @Override
    public OrderResponseDTO update(UUID orderId, String status) {
        Order order = orderRepository.findById(orderId).orElse(null);
        order.setStatus(status);
        orderRepository.save(order);
        return mapping(order);
    }

    @Override
    public OrderResponseDTO createOrderWithRetry(CreateOrderRequest request) {
        int attempts = 0;
        while (true) {
            try {
                return create(request);
            } catch (ObjectOptimisticLockingFailureException e) {
                attempts++;
                if (attempts >= 3) {
                    throw e;
                }
            }
        }
    }

    @Override
    public void delete(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        orderRepository.delete(order);
    }

    private OrderResponseDTO mapping(Order order) {
        return OrderResponseDTO.builder()
                .orderId(order.getOrderId())
                .orderDate(order.getOrderDate())
                .total(order.getTotal())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .createAt(order.getCreateAt())
                .customerId(order.getCustomer().getAccountId())
                .orderDetails(order.getOrderDetails()
                        .stream()
                        .map(od -> {
                            OrderDetailResponseDTO d = new OrderDetailResponseDTO();
                            d.setId(od.getId());
                            d.setQuantity(od.getQuantity());
                            d.setUnitPrice(od.getUnitPrice());
                            d.setSubTotal(od.getSubTotal());
                            d.setDiscount(od.getDiscount());
                            d.setProductId(od.getProduct().getProductUuid());
                            return d;
                        }).toList())
                .build();
    }
}
