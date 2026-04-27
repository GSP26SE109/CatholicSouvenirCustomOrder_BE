package org.example.catholicsouvenircustomorder.service.imp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.dto.request.CalculateShippingRequest;
import org.example.catholicsouvenircustomorder.dto.request.CheckoutRequest;
import org.example.catholicsouvenircustomorder.dto.request.CreateShipmentRequest;
import org.example.catholicsouvenircustomorder.dto.response.Order.CheckoutResponseDTO;
import org.example.catholicsouvenircustomorder.dto.response.Order.OrderDetailResponseDTO;
import org.example.catholicsouvenircustomorder.dto.response.Order.OrderDetailReviewDTO;
import org.example.catholicsouvenircustomorder.dto.response.Order.OrderResponseDTO;
import org.example.catholicsouvenircustomorder.dto.response.Order.OrderTemplateDetailResponseDTO;
import org.example.catholicsouvenircustomorder.dto.response.ShippingFeeResponse;
import org.example.catholicsouvenircustomorder.exception.BadRequestException;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.*;
import org.example.catholicsouvenircustomorder.service.CheckoutService;
import org.example.catholicsouvenircustomorder.service.ShippingService;
import org.example.catholicsouvenircustomorder.service.UserProfileService;
import org.springframework.data.redis.core.RedisTemplate;
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
    private final OrderGroupRepository orderGroupRepository;
    private final FeedbackRepository feedbackRepository;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ShippingService shippingService;
    private final UserProfileService userProfileService;
    
    @Override
    @Transactional
    public CheckoutResponseDTO checkout(UUID customerId, CheckoutRequest request) {
        validateCart(customerId);
        
        // Auto-fill recipient info from UserProfile if not provided
        if (request.getRecipientName() == null || request.getRecipientName().isBlank()) {
            try {
                var userProfile = userProfileService.getUserProfile(customerId);
                if (userProfile != null) {
                    if (request.getRecipientName() == null || request.getRecipientName().isBlank()) {
                        request.setRecipientName(userProfile.getFullName());
                    }
                    if (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
                        request.setPhoneNumber(userProfile.getPhone());
                    }
                    if (request.getShippingAddress() == null || request.getShippingAddress().isBlank()) {
                        request.setShippingAddress(userProfile.getAddress());
                    }
                    log.info("Auto-filled recipient info from UserProfile for customer {}", customerId);
                }
            } catch (Exception e) {
                log.warn("Could not auto-fill from UserProfile for customer {}: {}", customerId, e.getMessage());
            }
        }
        
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
        
        // Create OrderGroup with initial totalAmount = 0
        OrderGroup orderGroup = new OrderGroup();
        orderGroup.setCustomer(cart.getCustomer());
        orderGroup.setPaymentMethod(request.getPaymentMethod());
        orderGroup.setStatus("PENDING");
        orderGroup.setTotalAmount(BigDecimal.ZERO); // Set initial value to avoid NULL constraint
        orderGroup = orderGroupRepository.save(orderGroup);
        
        // Group items by artisan and create separate orders
        List<OrderResponseDTO> orders;
        if (hasProduct) {
            orders = createProductOrdersByArtisan(cart, request, orderGroup);
        } else {
            orders = createTemplateOrdersByArtisan(cart, request, orderGroup);
        }
        
        // Calculate total amount across all orders
        BigDecimal totalAmount = orders.stream()
                .map(OrderResponseDTO::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Update order group with actual total
        orderGroup.setTotalAmount(totalAmount);
        orderGroupRepository.save(orderGroup);
        
        // Clear cart after successful checkout
        cartItemRepository.deleteByCart_CartId(cart.getCartId());
        cart.clearItems();
        cartRepository.save(cart);
        
        // Invalidate Redis cache to ensure fresh data
        String cartKey = "cart:" + customerId;
        String countKey = "cart:" + customerId + ":count";
        redisTemplate.delete(cartKey);
        redisTemplate.delete(countKey);
        
        if (orders.isEmpty()) {
            throw new BadRequestException("Không thể tạo đơn hàng");
        }
        
        String message = orders.size() == 1 
            ? "Đã tạo 1 đơn hàng thành công. Vui lòng thanh toán để hoàn tất."
            : String.format("Đã tạo %d đơn hàng từ %d nghệ nhân khác nhau. Bạn chỉ cần thanh toán 1 lần cho tất cả đơn hàng.", 
                orders.size(), orders.size());
        
        log.info("Checkout completed: {} orders in group {} for customer {}", 
                orders.size(), orderGroup.getGroupId(), customerId);
        
        CheckoutResponseDTO response = CheckoutResponseDTO.builder()
                .orders(orders)
                .totalAmount(totalAmount)
                .orderCount(orders.size())
                .message(message)
                .build();
        
        // Add orderGroupId to response for payment
        response.setOrderGroupId(orderGroup.getGroupId());
        
        return response;
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
    
    private List<OrderResponseDTO> createProductOrdersByArtisan(Cart cart, CheckoutRequest request, OrderGroup orderGroup) {
        // Group cart items by artisan
        Map<UUID, List<CartItem>> itemsByArtisan = cart.getItems().stream()
                .filter(item -> item.getType() == CartItemType.PRODUCT)
                .collect(Collectors.groupingBy(item -> item.getProduct().getArtisan().getArtisanUuid()));
        
        List<OrderResponseDTO> orders = new ArrayList<>();
        
        for (Map.Entry<UUID, List<CartItem>> entry : itemsByArtisan.entrySet()) {
            UUID artisanId = entry.getKey();
            List<CartItem> items = entry.getValue();
            
            BigDecimal productTotal = BigDecimal.ZERO;
            List<OrderDetail> orderDetails = new ArrayList<>();
            
            for (CartItem item : items) {
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
                productTotal = productTotal.add(item.getSubtotal());
            }
            
            // Calculate shipping fee
            Integer weight = request.getWeight() != null ? request.getWeight() : items.size() * 500;
            CreateShipmentRequest shipmentRequest = CreateShipmentRequest.builder()
                    .toDistrictId(request.getToDistrictId())
                    .toWardCode(request.getToWardCode())
                    .weight(weight)
                    .length(request.getLength() != null ? request.getLength() : 20)
                    .width(request.getWidth() != null ? request.getWidth() : 20)
                    .height(request.getHeight() != null ? request.getHeight() : 10)
                    .orderValue(productTotal)
                    .build();
            
            BigDecimal shippingFee;
            try {
                shippingFee = shippingService.calculateShippingFee(shipmentRequest);
            } catch (Exception e) {
                log.error("GHN API error for artisan {}, using default fee: {}", artisanId, e.getMessage());
                shippingFee = BigDecimal.valueOf(30000);
            }
            
            BigDecimal total = productTotal.add(shippingFee);
            
            // Create order for this artisan
            Order order = new Order();
            order.setCustomer(cart.getCustomer());
            order.setOrderGroup(orderGroup); // Link to order group
            order.setOrderDate(LocalDateTime.now());
            order.setTotal(total); // Now includes shipping fee
            order.setStatus("PENDING");
            order.setPaymentMethod(request.getPaymentMethod());
            order.setShippingFee(shippingFee); // Store shipping fee
            order.setCreateAt(LocalDateTime.now());
            order.setUpdateAt(LocalDateTime.now());
            
            order = orderRepository.save(order);
            
            // Save order details
            for (OrderDetail detail : orderDetails) {
                detail.setOrder(order);
            }
            order.setOrderDetails(orderDetails);
            orderDetailRepository.saveAll(orderDetails);
            
            log.info("Created product order {} for artisan {} with {} items, shipping fee: {}", 
                    order.getOrderId(), artisanId, items.size(), shippingFee);
            
            orders.add(mapToOrderResponse(order));
        }
        
        return orders;
    }
    
    private List<OrderResponseDTO> createTemplateOrdersByArtisan(Cart cart, CheckoutRequest request, OrderGroup orderGroup) {
        // Group cart items by artisan
        Map<UUID, List<CartItem>> itemsByArtisan = cart.getItems().stream()
                .filter(item -> item.getType() == CartItemType.TEMPLATE)
                .collect(Collectors.groupingBy(item -> item.getTemplate().getArtisan().getArtisanUuid()));
        
        List<OrderResponseDTO> orders = new ArrayList<>();
        
        for (Map.Entry<UUID, List<CartItem>> entry : itemsByArtisan.entrySet()) {
            UUID artisanId = entry.getKey();
            List<CartItem> items = entry.getValue();
            
            BigDecimal total = BigDecimal.ZERO;
            List<OrderTemplateDetail> templateDetails = new ArrayList<>();
            
            for (CartItem item : items) {
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
            
            // Create order for this artisan
            Order order = new Order();
            order.setCustomer(cart.getCustomer());
            order.setOrderGroup(orderGroup); // Link to order group
            order.setOrderDate(LocalDateTime.now());
            order.setTotal(total);
            order.setStatus("PENDING");
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
            
            log.info("Created template order {} for artisan {} with {} items", 
                    order.getOrderId(), artisanId, items.size());
            
            orders.add(mapToOrderResponse(order));
        }
        
        return orders;
    }
    
    private OrderResponseDTO mapToOrderResponse(Order order) {
        List<OrderDetailResponseDTO> orderDetails = new ArrayList<>();
        List<OrderTemplateDetailResponseDTO> templateDetails = new ArrayList<>();
        
        // Map product details
        if (order.getOrderDetails() != null) {
            orderDetails = order.getOrderDetails().stream()
                .map(detail -> {
                    String imageUrl = null;
                    if (detail.getProduct().getImages() != null && !detail.getProduct().getImages().isEmpty()) {
                        imageUrl = detail.getProduct().getImages().get(0).getImage_url();
                    }
                    
                    // Load feedback for this order detail
                    OrderDetailReviewDTO review = feedbackRepository.findByOrderDetail(detail.getId())
                        .map(feedback -> OrderDetailReviewDTO.builder()
                            .feedbackId(feedback.getFeedbackId())
                            .rating(feedback.getRating())
                            .comment(feedback.getComment())
                            .createdAt(feedback.getCreatedAt())
                            .build())
                        .orElse(null);
                    
                    return OrderDetailResponseDTO.builder()
                        .id(detail.getId())
                        .quantity(detail.getQuantity())
                        .unitPrice(detail.getUnitPrice())
                        .subTotal(detail.getSubTotal())
                        .discount(detail.getDiscount())
                        .productId(detail.getProduct().getProductId())
                        .productName(detail.getProduct().getProductName())
                        .image(imageUrl)
                        .review(review)
                        .build();
                })
                .collect(Collectors.toList());
        }
        
        // Map template details
        if (order.getTemplateDetails() != null) {
            templateDetails = order.getTemplateDetails().stream()
                .map(detail -> {
                    String imageUrl = null;
                    if (detail.getTemplate().getBaseImages() != null && !detail.getTemplate().getBaseImages().isEmpty()) {
                        imageUrl = detail.getTemplate().getBaseImages().get(0);
                    }
                    return OrderTemplateDetailResponseDTO.builder()
                        .id(detail.getOrderTemplateDetailId())
                        .quantity(detail.getQuantity())
                        .unitPrice(detail.getUnitPrice())
                        .subtotal(detail.getSubtotal())
                        .templateId(detail.getTemplate().getTemplateId())
                        .templateName(detail.getTemplate().getName())
                        .image(imageUrl)
                        .customizations(detail.getCustomizations())
                        .build();
                })
                .collect(Collectors.toList());
        }
        
        return OrderResponseDTO.builder()
            .orderId(order.getOrderId())
            .orderDate(order.getOrderDate())
            .total(order.getTotal())
            .shippingFee(order.getShippingFee())
            .status(order.getStatus())
            .paymentMethod(order.getPaymentMethod())
            .createAt(order.getCreateAt())
            .updateAt(order.getUpdateAt())
            .customerId(order.getCustomer().getAccountId())
            .fullName(order.getCustomer().getFullName())
            .orderDetails(orderDetails)
            .templateDetails(templateDetails)
            .build();
    }
    
    @Override
    public ShippingFeeResponse calculateShippingFeeForCart(UUID customerId, CalculateShippingRequest request) {
        // 1. Get customer's cart
        Cart cart = cartRepository.findByCustomer_AccountId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giỏ hàng"));
        
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Giỏ hàng trống");
        }
        
        // 2. Group items by artisan
        Map<UUID, List<CartItem>> itemsByArtisan = cart.getItems().stream()
                .collect(Collectors.groupingBy(item -> 
                    item.getProduct() != null 
                        ? item.getProduct().getArtisan().getArtisanUuid()
                        : item.getTemplate().getArtisan().getArtisanUuid()
                ));
        
        // 3. Calculate shipping fee for each artisan
        List<ShippingFeeResponse.ArtisanShippingBreakdown> breakdown = new ArrayList<>();
        BigDecimal totalShippingFee = BigDecimal.ZERO;
        Integer totalWeight = 0;
        
        for (Map.Entry<UUID, List<CartItem>> entry : itemsByArtisan.entrySet()) {
            UUID artisanId = entry.getKey();
            List<CartItem> items = entry.getValue();
            
            // Calculate weight (default 500g per item)
            Integer artisanWeight = request.getWeight() != null 
                ? request.getWeight() 
                : items.size() * 500;
            totalWeight += artisanWeight;
            
            // Calculate total value
            BigDecimal artisanTotal = items.stream()
                    .map(CartItem::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Create shipping fee request
            CreateShipmentRequest shipmentRequest = CreateShipmentRequest.builder()
                    .toDistrictId(request.getToDistrictId())
                    .toWardCode(request.getToWardCode())
                    .weight(artisanWeight)
                    .length(request.getLength() != null ? request.getLength() : 20)
                    .width(request.getWidth() != null ? request.getWidth() : 20)
                    .height(request.getHeight() != null ? request.getHeight() : 10)
                    .orderValue(artisanTotal)
                    .build();
            
            // Call GHN API (with fallback to default fee)
            BigDecimal shippingFee;
            try {
                shippingFee = shippingService.calculateShippingFee(shipmentRequest);
            } catch (Exception e) {
                log.error("GHN API error, using default fee: {}", e.getMessage());
                shippingFee = BigDecimal.valueOf(30000); // Default 30k VND
            }
            
            totalShippingFee = totalShippingFee.add(shippingFee);
            
            // Get artisan info
            String artisanName = items.get(0).getProduct() != null
                    ? items.get(0).getProduct().getArtisan().getArtisanName()
                    : items.get(0).getTemplate().getArtisan().getArtisanName();
            
            List<String> productNames = items.stream()
                    .map(item -> item.getProduct() != null 
                        ? item.getProduct().getProductName()
                        : item.getTemplate().getName())
                    .collect(Collectors.toList());
            
            breakdown.add(ShippingFeeResponse.ArtisanShippingBreakdown.builder()
                    .artisanId(artisanId)
                    .artisanName(artisanName)
                    .shippingFee(shippingFee)
                    .weight(artisanWeight)
                    .itemCount(items.size())
                    .productNames(productNames)
                    .build());
        }
        
        return ShippingFeeResponse.builder()
                .totalShippingFee(totalShippingFee)
                .totalWeight(totalWeight)
                .breakdown(breakdown)
                .build();
    }
}
