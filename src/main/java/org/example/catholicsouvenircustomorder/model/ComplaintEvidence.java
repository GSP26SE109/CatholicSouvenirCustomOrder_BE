package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(name="complant_evidence",
        indexes = {
                @Index(name = "idx_complaint_uploaded", columnList = "complaint_id, uploaded_by")
        })
public class ComplaintEvidence {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne
    @JoinColumn(name = "complaint_id")
    private Complaint complaint;

    @ElementCollection
    @CollectionTable(name = "complaint_evidence_images", joinColumns = @JoinColumn(name = "complant_evidence_id"))
    @Column(name = "image_url")
    private List<String> imageUrl = new ArrayList<>();
    private String message;

    private UUID uploadedBy;
    private LocalDateTime createdAt;
}