package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(
        name = "complaints",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "order_id")
        }
)
public class Complaint {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID complaintId;

    @OneToOne
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;
    private UUID customerId;
    private UUID artisanId;
    @Enumerated(EnumType.STRING)
    private ComplaintType type;
    @Enumerated(EnumType.STRING)
    private ComplaintStatus status;
    private BigDecimal refundAmount;

    private LocalDateTime createdAt;
    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ComplaintEvidence> evidences = new ArrayList<>();
}
