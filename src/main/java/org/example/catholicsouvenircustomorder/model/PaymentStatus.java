package org.example.catholicsouvenircustomorder.model;

public enum PaymentStatus {
    PENDING,           // Chờ thanh toán
    PROCESSING,        // Đang xử lý
    SUCCESS,           // Thanh toán thành công
    FAILED,            // Thanh toán thất bại
    CANCELLED,         // Đã hủy
    REFUNDED           // Đã hoàn tiền
}
