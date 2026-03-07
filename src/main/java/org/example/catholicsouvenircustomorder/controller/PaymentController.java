package org.example.catholicsouvenircustomorder.controller;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.model.Order;
import org.example.catholicsouvenircustomorder.model.Payment;
import org.example.catholicsouvenircustomorder.model.Stage;
import org.example.catholicsouvenircustomorder.service.imp.PaymentServiceImp;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentServiceImp paymentService;

    @PostMapping("/webhook")
    @Transactional
    public ResponseEntity<Void> webhook(@RequestBody Map<String, Object> payload) {

        String orderCode = payload.get("orderCode").toString();
        String status = payload.get("status").toString();
        String transactionId = payload.get("id").toString();

        Payment payment = paymentService
                .findById(UUID.fromString(orderCode));

        if (payment.getStatus().equals("SUCCESS"))
            return ResponseEntity.ok().build();

        if ("PAID".equals(status)) {

            payment.setStatus("SUCCESS");
            payment.setProviderTransactionId(transactionId);

            Order order = payment.getOrder();

            if (payment.getStage() != null) {
                Stage stage = payment.getStage();
                stage.setStatus("PAID");
                order.setStatus("PARTIALLY_PAID");
            } else {
                order.setStatus("PAID");
            }
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{orderId}")
    public ResponseEntity<String> createPayment(@PathVariable String orderId,
                                                @RequestParam(required = false) String stageId) {
        String paymentUrl = paymentService.createPaymentByPayOS(UUID.fromString(orderId), UUID.fromString(stageId));
        return ResponseEntity.ok(paymentUrl);
    }
}
