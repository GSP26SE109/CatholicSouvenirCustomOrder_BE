package org.example.catholicsouvenircustomorder.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCallbackRequest {
    private Map<String, String> params;
    private String paymentGateway; // "VNPAY" or "ZALOPAY"
}
