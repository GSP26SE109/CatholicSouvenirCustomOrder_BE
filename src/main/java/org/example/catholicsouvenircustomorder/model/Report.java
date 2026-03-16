package org.example.catholicsouvenircustomorder.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Entity
@Data
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID reportId;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    private String reportDescription;
    private List<String> image_url;
}
