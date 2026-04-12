package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.Artisan;
import org.example.catholicsouvenircustomorder.model.Conversation;
import org.example.catholicsouvenircustomorder.model.CustomRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    
    // Find conversation by request and artisan
    Optional<Conversation> findByRequestAndArtisan(CustomRequest request, Artisan artisan);
    
    // Check if conversation exists
    boolean existsByRequestAndArtisan(CustomRequest request, Artisan artisan);
    
    // Get all conversations for a request (customer view)
    List<Conversation> findByRequestOrderByUpdatedAtDesc(CustomRequest request);
    
    // Get all conversations for an artisan
    @Query("SELECT c FROM Conversation c WHERE c.artisan.artisanUuid = :artisanId ORDER BY c.updatedAt DESC")
    List<Conversation> findByArtisanIdOrderByUpdatedAtDesc(@Param("artisanId") UUID artisanId);
    
    // Get all conversations where user is participant (customer or artisan)
    @Query("SELECT c FROM Conversation c WHERE c.customer.accountId = :userId OR c.artisan.account.accountId = :userId ORDER BY c.updatedAt DESC")
    List<Conversation> findByCustomerAccountIdOrArtisanAccountAccountId(@Param("userId") UUID userId1, @Param("userId") UUID userId2);
    
    // Count interested artisans for a request
    long countByRequest(CustomRequest request);
}
