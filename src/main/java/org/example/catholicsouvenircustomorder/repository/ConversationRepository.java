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
    List<Conversation> findByCustomerAccountIdOrArtisanAccountAccountId(@Param("userId") UUID userId);
    
    // Count interested artisans for a request
    long countByRequest(CustomRequest request);
    
    // ==================== Artisan Dashboard Statistics Methods ====================
    
    /**
     * Get average response time in hours for an artisan
     * Calculates the average time between custom request creation and first artisan message
     * Requirements: 3.3, 7.6
     */
    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (first_message_time - cr.created_at)) / 3600) " +
           "FROM custom_requests cr " +
           "JOIN conversations c ON cr.conversation_id = c.conversation_id " +
           "JOIN artisans a ON cr.selected_artisan_id = a.artisan_id " +
           "CROSS JOIN LATERAL ( " +
           "  SELECT MIN(cm.sent_at) as first_message_time " +
           "  FROM chat_messages cm " +
           "  JOIN accounts acc ON cm.sender_id = acc.account_id " +
           "  JOIN artisans a2 ON acc.artisan_profile_id = a2.artisan_id " +
           "  WHERE cm.conversation_id = c.conversation_id " +
           "  AND a2.artisan_uuid = :artisanId " +
           ") first_msg " +
           "WHERE a.artisan_uuid = :artisanId " +
           "AND first_message_time IS NOT NULL",
           nativeQuery = true)
    Double getAvgResponseTimeHours(@Param("artisanId") UUID artisanId);
}
