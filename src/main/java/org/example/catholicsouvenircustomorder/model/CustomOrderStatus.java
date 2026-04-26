package org.example.catholicsouvenircustomorder.model;

public enum CustomOrderStatus {
    PENDING_CONFIRMATION, // Chờ customer xác nhận (artisan vừa tạo order)
    PENDING_PAYMENT,   // Chờ thanh toán (customer đã xác nhận)
    CONFIRMED,         // Đã xác nhận (đã thanh toán)
    IN_PROGRESS,       // Đang thực hiện (có ít nhất 1 stage đã thanh toán)
    IN_PRODUCTION,     // Đang sản xuất
    SHIPPING,          // Đang vận chuyển
    DELIVERED,         // Đã giao hàng (chờ customer xác nhận)
    COMPLETED,         // Hoàn thành (customer đã xác nhận)
    CANCELLED,         // Đã hủy
    REFUNDED           // Đã hoàn tiền
}
