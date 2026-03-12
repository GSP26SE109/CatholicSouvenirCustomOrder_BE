package org.example.catholicsouvenircustomorder.Utils.Helper;

import org.example.catholicsouvenircustomorder.dto.request.OrderDTO.CreateOrderRequest;
import org.example.catholicsouvenircustomorder.dto.request.OrderDTO.OrderItemRequest;
import org.example.catholicsouvenircustomorder.dto.response.Order.OrderDetailResponseDTO;
import org.example.catholicsouvenircustomorder.dto.response.Order.OrderResponseDTO;
import org.example.catholicsouvenircustomorder.model.Order;
import org.example.catholicsouvenircustomorder.model.OrderDetail;
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
}
