package org.example.catholicsouvenircustomorder.model;

public enum ComplaintStatus {
    PENDING,           // Chờ xử lý
    WAITING_RETURN,    // Chờ trả hàng (nếu requireReturn = true)
    PROCESSING_REFUND, // Đang xử lý hoàn tiền
    APPROVED,          // Đã phê duyệt và hoàn tiền thành công
    REJECTED           // Đã từ chối
}
