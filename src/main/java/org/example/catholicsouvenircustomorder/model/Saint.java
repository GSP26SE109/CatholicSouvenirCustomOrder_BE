package org.example.catholicsouvenircustomorder.model;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Data
public class Saint {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID saintId;
    private String saintName;
    private String saintDescription;
    private LocalDateTime saintDate;


    @OneToMany(mappedBy = "saint")
    private List<Account> account;

}
