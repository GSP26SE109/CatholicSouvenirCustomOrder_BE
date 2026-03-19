package org.example.catholicsouvenircustomorder.model;

public enum QuotationStatus {
    DRAFT,      // Báo giá nháp
    SENT,       // Đã gửi cho customer
    REVISED,    // Đã chỉnh sửa
    FINAL,      // Báo giá cuối cùng
    ACCEPTED,   // Customer chấp nhận
    REJECTED    // Customer từ chối
}
