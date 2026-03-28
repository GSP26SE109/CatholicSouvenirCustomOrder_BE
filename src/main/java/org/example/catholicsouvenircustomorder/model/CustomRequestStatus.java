package org.example.catholicsouvenircustomorder.model;

public enum CustomRequestStatus {
    PENDING,           // Chờ nghệ nhân chấp nhận (Template-Based)
    OPEN,              // Mở cho nghệ nhân gửi báo giá (Request-Based)
    NEGOTIATING,       // Đang thương lượng (Request-Based)
    ACCEPTED,          // Đã chấp nhận
    REJECTED,          // Bị từ chối
    IN_PROGRESS,       // Đang thực hiện
    COMPLETED,         // Hoàn thành
    CANCELLED          // Đã hủy
}
