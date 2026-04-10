package org.example.catholicsouvenircustomorder.service.imp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.request.CheckoutRequest;
import org.example.catholicsouvenircustomorder.dto.response.Order.OrderDetailResponseDTO;
import org.example.catholicsouvenircustomorder.dto.response.Order.OrderResponseDTO;
import org.example.catholicsouvenircustomorder.exception.BadRequestException;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.*;
import org.example.catholicsouvenircustomorder.service.CheckoutService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutServiceImp implements CheckoutService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final OrderTemplateDetailRepository orderTemplateDetailRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public OrderResponseDTO checkout(UUID customerId, CheckoutRequest request) {
        validateCart(customerId);

        Cart cart = cartRepository.findByCustomer_AccountId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giỏ hàng"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Giỏ hàng trống");
        }

        boolean hasProduct = cart.getItems().stream()
                .anyMatch(item -> item.getType() == CartItemType.PRODUCT);
        boolean hasTemplate = cart.getItems().stream()
                .anyMatch(item -> item.getType() == CartItemType.TEMPLATE);

        if (hasProduct && hasTemplate) {
            throw new BadRequestException(
                    "Không thể checkout cùng lúc sản phẩm có sẵn và sản phẩm custom");
        }

        OrderResponseDTO orderResponse;
        if (hasProduct) {
            orderResponse = createProductOrder(cart, request);
        } else {
            orderResponse = createTemplateOrder(cart, request);
        }

        // Clear cart after successful checkout
        cartItemRepository.deleteByCart_CartId(cart.getCartId());
        cart.clearItems();
        cartRepository.save(cart);

        return orderResponse;
    }

    @Override
    public void validateCart(UUID customerId) {
        Cart cart = cartRepository.findByCustomer_AccountId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giỏ hàng"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Giỏ hàng trống");
        }

        for (CartItem item : cart.getItems()) {
            if (item.getType() == CartItemType.PRODUCT) {
                Product product = item.getProduct();
                if (product.getQuantity() < item.getQuantity()) {
                    throw new BadRequestException(
                            String.format("Sản phẩm '%s' không đủ hàng", product.getProductName()));
                }
            }
        }
    }

    private OrderResponseDTO createProductOrder(Cart cart, CheckoutRequest request) {
        BigDecimal total = BigDecimal.ZERO;
        List<OrderDetail> orderDetails = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            if (item.getType() != CartItemType.PRODUCT) continue;

            Product product = item.getProduct();

            // Update product quantity
            if (product.getQuantity() < item.getQuantity()) {
                throw new BadRequestException(
                        String.format("Sản phẩm '%s' không đủ hàng", product.getProductName()));
            }
            product.setQuantity(product.getQuantity() - item.getQuantity());
            productRepository.save(product);

            // Create order detail
            OrderDetail detail = new OrderDetail();
            detail.setProduct(product);
            detail.setQuantity(item.getQuantity());
            detail.setUnitPrice(item.getPrice());
            detail.setSubTotal(item.getSubtotal());
            detail.setDiscount(0);

            orderDetails.add(detail);
            total = total.add(item.getSubtotal());
        }

        // Create order
        Order order = new Order();
        order.setCustomer(cart.getCustomer());
        order.setOrderDate(LocalDateTime.now());
        order.setTotal(total);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setCreateAt(LocalDateTime.now());
        order.setUpdateAt(LocalDateTime.now());

        order = orderRepository.save(order);

        // Save order details
        for (OrderDetail detail : orderDetails) {
            detail.setOrder(order);
        }
        order.setOrderDetails(orderDetails);
        orderDetailRepository.saveAll(orderDetails);

        log.info("Created product order {} for customer {}", order.getOrderId(), cart.getCustomer().getAccountId());

        return mapToOrderResponse(order);
    }

    private OrderResponseDTO createTemplateOrder(Cart cart, CheckoutRequest request) {
        BigDecimal total = BigDecimal.ZERO;
        List<OrderTemplateDetail> templateDetails = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            if (item.getType() != CartItemType.TEMPLATE) continue;

            ProductTemplate template = item.getTemplate();

            // Parse customization data
            Map<String, String> customizations = null;
            if (item.getCustomizationData() != null) {
                try {
                    customizations = objectMapper.readValue(
                            item.getCustomizationData(),
                            new TypeReference<Map<String, String>>() {}
                    );
                } catch (JsonProcessingException e) {
                    throw new BadRequestException("Dữ liệu customization không hợp lệ");
                }
            }

            // Create template detail
            OrderTemplateDetail detail = new OrderTemplateDetail();
            detail.setTemplate(template);
            detail.setCustomizations(customizations);
            detail.setQuantity(item.getQuantity());
            detail.setUnitPrice(item.getPrice());
            detail.setSubtotal(item.getSubtotal());

            templateDetails.add(detail);
            total = total.add(item.getSubtotal());
        }

        // Create order
        Order order = new Order();
        order.setCustomer(cart.getCustomer());
        order.setOrderDate(LocalDateTime.now());
        order.setTotal(total);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setCreateAt(LocalDateTime.now());
        order.setUpdateAt(LocalDateTime.now());

        order = orderRepository.save(order);

        // Save template details
        for (OrderTemplateDetail detail : templateDetails) {
            detail.setOrder(order);
        }
        order.setTemplateDetails(templateDetails);
        orderTemplateDetailRepository.saveAll(templateDetails);

        log.info("Created template order {} for customer {}", order.getOrderId(), cart.getCustomer().getAccountId());

        return mapToOrderResponse(order);
    }

    private OrderResponseDTO mapToOrderResponse(Order order) {
        List<OrderDetailResponseDTO> details = new ArrayList<>();

        // Map product details
        if (order.getOrderDetails() != null) {
            details.addAll(order.getOrderDetails().stream()
                    .map(detail -> OrderDetailResponseDTO.builder()
                            .id(detail.getId())
                            .quantity(detail.getQuantity())
                            .unitPrice(detail.getUnitPrice())
                            .subTotal(detail.getSubTotal())
                            .discount(detail.getDiscount())
                            .productId(detail.getProduct().getProductId())
                            .productName(detail.getProduct().getProductName())
                            .build())
                    .collect(Collectors.toList()));
        }

        // Map template details (as order details for consistency)
        if (order.getTemplateDetails() != null) {
            details.addAll(order.getTemplateDetails().stream()
                    .map(detail -> OrderDetailResponseDTO.builder()
                            .id(detail.getOrderTemplateDetailId())
                            .quantity(detail.getQuantity())
                            .unitPrice(detail.getUnitPrice())
                            .subTotal(detail.getSubtotal())
                            .discount(0)
                            .productId(detail.getTemplate().getTemplateId())
                            .productName(detail.getTemplate().getName() + " (Custom)")
                            .build())
                    .collect(Collectors.toList()));
        }

        return OrderResponseDTO.builder()
                .orderId(order.getOrderId())
                .orderDate(order.getOrderDate())
                .total(order.getTotal())
                .status(String.valueOf(order.getStatus()))
                .paymentMethod(order.getPaymentMethod())
                .createAt(order.getCreateAt())
                .updateAt(order.getUpdateAt())
                .customerId(order.getCustomer().getAccountId())
                .fullName(order.getCustomer().getFullName())
                .orderDetails(details)
                .build();
    }
}
