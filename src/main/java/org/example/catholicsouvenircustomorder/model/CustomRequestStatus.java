package org.example.catholicsouvenircustomorder.model;

public enum CustomRequestStatus {
    DRAFT,             // Customer tạo nhưng chưa publish
    OPEN,              // Published, artisans có thể chat
    ARTISAN_SELECTED,  // Customer đã chọn artisan
    IN_PROGRESS,       // Order đã tạo, đang làm việc
    COMPLETED,         // Hoàn thành
    CANCELLED          // Đã hủy
}
