package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.Account;
import org.example.catholicsouvenircustomorder.model.Artisan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArtisanRepository extends JpaRepository<Artisan, UUID> {
    boolean existsByAccount(Account account);
    
    @Query("SELECT a FROM Artisan a JOIN a.customOrders co WHERE co.customOrderId = :customOrderId")
    Optional<Artisan> findByCustomOrderId(@Param("customOrderId") UUID customOrderId);
}
