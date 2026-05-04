package org.example.catholicsouvenircustomorder.model;

public enum RefundStatus {
    PENDING,             // Chờ xử lý
    PROCESSING,          // Đã gửi yêu cầu hoàn tiền VNPay
    COMPLETED,           // Hoàn thành
    FAILED,              // Thất bại
    PARTIALLY_REFUNDED,  // Hoàn tiền một phần (cho CustomOrder với nhiều stage payments)
    RECOVERED            // Đã thu hồi tiền từ Artisan (cho insurance fund)
}
