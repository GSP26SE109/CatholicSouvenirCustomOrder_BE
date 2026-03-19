package org.example.catholicsouvenircustomorder.model;

public enum CustomRequestStatus {
    PENDING,           // Customer vừa tạo request
    ARTISAN_SELECTED,  // Customer đã chọn artisan
    NEGOTIATING,       // Đang trao đổi báo giá
    ARTISAN_CONFIRMED, // Artisan đã được chốt
    ORDER_CREATED,     // Order đã được tạo
    CANCELLED          // Request bị hủy
}
