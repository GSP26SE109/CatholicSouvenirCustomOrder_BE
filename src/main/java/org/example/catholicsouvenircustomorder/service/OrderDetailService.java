package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.model.OrderDetail;

import java.util.List;
import java.util.UUID;

public interface OrderDetailService {
    List<OrderDetail> findAll();
    OrderDetail findById(UUID id);
    OrderDetail create(OrderDetail order);
    OrderDetail update(UUID orderDetailId,OrderDetail orderDetail);
    void delete(UUID orderDetailId);
}
