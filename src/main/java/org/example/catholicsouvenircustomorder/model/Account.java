package org.example.catholicsouvenircustomorder.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Data
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID accountId;

    private String fullName;
    private String email;
    private String password;
    private String phone;
    private String gender;
    private String dateOfBirth;
    private String avt_url;

    private String verificationToken;
    private boolean isVerified = false;



    @Column(name = "created_date")
    private LocalDateTime createdDate;
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne
    @JoinColumn(name = "saint_id")
    private Saint saint;

    @OneToMany(mappedBy = "customer")
    @JsonIgnore
    private List<Order> orders;

    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL)
    @JsonIgnore
    private Artisan artisanProfile;

    @OneToMany(mappedBy = "account")
    @JsonIgnore
    private List<Product> productList;



}
