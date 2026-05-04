package org.example.catholicsouvenircustomorder.util;

import org.example.catholicsouvenircustomorder.dto.request.OrderDTO.CreateOrderRequest;
import org.example.catholicsouvenircustomorder.dto.request.OrderDTO.OrderItemRequest;
import org.example.catholicsouvenircustomorder.dto.response.Order.OrderDetailResponseDTO;
import org.example.catholicsouvenircustomorder.dto.response.Order.OrderDetailReviewDTO;
import org.example.catholicsouvenircustomorder.dto.response.Order.OrderResponseDTO;
import org.example.catholicsouvenircustomorder.model.Order;
import org.example.catholicsouvenircustomorder.model.OrderDetail;
import org.example.catholicsouvenircustomorder.model.ProductImage;
import org.mapstruct.*;
import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "customerId", source = "customer.accountId")
    @Mapping(target = "fullName", source = "customer.fullName")
    @Mapping(target = "orderDetails", source = "orderDetails")
    OrderResponseDTO toResponse(Order order);

    List<OrderResponseDTO> toResponseList(List<Order> orders);

    @Mapping(target = "productId", source = "product.productId")
    @Mapping(target = "productName", source = "product.productName")
    @Mapping(target = "image", source = "product.images")
    @Mapping(target = "review", source = "feedbacks", qualifiedByName = "mapFeedbacksToReview")
    OrderDetailResponseDTO toDetailResponse(OrderDetail detail);

    List<OrderDetailResponseDTO> toDetailResponseList(List<OrderDetail> details);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "orderId", ignore = true)
    @Mapping(target = "orderDetails", source = "items")
    Order toEntity(CreateOrderRequest dto);


    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "product", ignore = true)
    OrderDetail toOrderDetail(OrderItemRequest dto);

    @AfterMapping
    default void linkOrderDetails(@MappingTarget Order order) {
        if (order.getOrderDetails() != null) {
            order.getOrderDetails()
                    .forEach(detail -> detail.setOrder(order));
        }
    }
    
    // Custom mapping method to convert List<ProductImage> to String
    default String map(List<ProductImage> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.get(0).getImage_url();
    }
    
    // Custom mapping method to convert List<Feedback> to OrderDetailReviewDTO
    @Named("mapFeedbacksToReview")
    default OrderDetailReviewDTO mapFeedbacksToReview(List<org.example.catholicsouvenircustomorder.model.Feedback> feedbacks) {
        if (feedbacks == null || feedbacks.isEmpty()) {
            return null;
        }
        org.example.catholicsouvenircustomorder.model.Feedback feedback = feedbacks.get(0);
        return OrderDetailReviewDTO.builder()
                .feedbackId(feedback.getFeedbackId())
                .rating(feedback.getRating())
                .comment(feedback.getComment())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}
