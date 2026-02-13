package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
}

