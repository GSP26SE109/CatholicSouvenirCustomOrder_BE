package org.example.catholicsouvenircustomorder.service.imp;

import org.example.catholicsouvenircustomorder.model.OrderDetail;
import org.example.catholicsouvenircustomorder.service.OrderDetailService;

import java.util.List;
import java.util.UUID;

public class OrderDetailServiceImp implements OrderDetailService {
    @Override
    public List<OrderDetail> findAll() {
        return List.of();
    }

    @Override
    public OrderDetail findById(UUID id) {
        return null;
    }

    @Override
    public OrderDetail create(OrderDetail order) {
        return null;
    }

    @Override
    public OrderDetail update(UUID orderDetailId, OrderDetail orderDetail) {
        return null;
    }

    @Override
    public void delete(UUID orderDetailId) {

    }
}
