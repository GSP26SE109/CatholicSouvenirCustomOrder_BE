package org.example.catholicsouvenircustomorder.model;

public enum PaymentType {
    DEPOSIT_PAYMENT,         // Đặt cọc ban đầu (stage đầu tiên)
    PROGRESS_PAYMENT,        // Thanh toán tiến độ (các stage giữa)
    FINAL_PAYMENT           // Thanh toán cuối cùng (stage cuối)
}
