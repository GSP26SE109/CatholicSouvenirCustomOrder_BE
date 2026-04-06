package org.example.catholicsouvenircustomorder.model;

public enum ShippingStatus {
    PENDING,           // Chờ lấy hàng
    PICKING,           // Đang lấy hàng
    PICKED,            // Đã lấy hàng
    STORING,           // Nhập kho
    TRANSPORTING,      // Đang vận chuyển
    DELIVERING,        // Đang giao hàng
    DELIVERED,         // Đã giao hàng
    RETURNED,          // Hoàn trả
    CANCELLED          // Đã hủy
}
