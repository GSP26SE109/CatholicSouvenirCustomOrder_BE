package org.example.catholicsouvenircustomorder.model;

public enum CustomOrderStatus {
    PENDING,           // Đơn hàng mới tạo
    IN_PROGRESS,       // Đang thực hiện
    COMPLETED,         // Hoàn thành sản xuất
    SHIPPING,          // Đang giao hàng
    DELIVERED,         // Đã giao hàng
    CANCELLED,         // Đã hủy
    REFUNDED           // Đã hoàn tiền
}
