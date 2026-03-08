package org.example.catholicsouvenircustomorder.service.imp;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.model.Order;
import org.example.catholicsouvenircustomorder.model.Payment;
import org.example.catholicsouvenircustomorder.model.Stage;
import org.example.catholicsouvenircustomorder.repository.OrderRepository;
import org.example.catholicsouvenircustomorder.repository.PaymentRepository;
import org.example.catholicsouvenircustomorder.repository.ProductRepository;
import org.example.catholicsouvenircustomorder.repository.StageRepository;
import org.example.catholicsouvenircustomorder.service.PaymentService;
import org.example.catholicsouvenircustomorder.service.thirdParty.PayOSClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class PaymentServiceImp implements PaymentService {
    private final OrderRepository orderRepository;
    private final StageRepository stageRepository;
    private final PaymentRepository paymentRepository;
    private final PayOSClient payOSClient;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public String createPaymentByPayOS(UUID orderId, UUID stageId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow();
        Stage stage=null;
        BigDecimal amount;
        if (stageId != null) {
            stage = stageRepository.findById(stageId)
                    .orElseThrow();

            amount = stage.getAmount();
        } else {
            amount = order.getTotal();
        }
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setStage(stage);
        payment.setAmount(amount);
        payment.setStatus("PENDING");
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        return payOSClient.createPaymentLink(payment);
    }

    @Override
    public Payment findById(UUID paymentId) {
        return paymentRepository.findById(paymentId).orElse(null);
    }
}
