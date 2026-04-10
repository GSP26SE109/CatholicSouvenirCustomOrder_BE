package org.example.catholicsouvenircustomorder.model;

public enum CustomOrderStatus {
    PENDING_PAYMENT,   // Chờ thanh toán
    CONFIRMED,         // Đã xác nhận (đã thanh toán)
    IN_PROGRESS,       // Đang thực hiện (có ít nhất 1 stage đã thanh toán)
    IN_PRODUCTION,     // Đang sản xuất
    COMPLETED,         // Hoàn thành
    CANCELLED          // Đã hủy
}
