package org.example.catholicsouvenircustomorder.model;

public enum StageStatus {
    PENDING,           // Chưa bắt đầu
    IN_PROGRESS,       // Đang làm
    WAITING_PAYMENT,   // Chờ thanh toán
    PAID,              // Đã thanh toán
    COMPLETED          // Hoàn thành
}
