package org.example.catholicsouvenircustomorder.repository;

import org.example.catholicsouvenircustomorder.model.Account;
import org.example.catholicsouvenircustomorder.model.ApplicationStatus;
import org.example.catholicsouvenircustomorder.model.ArtisanApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ArtisanApplicationRepository extends JpaRepository<ArtisanApplication, UUID> {
    List<ArtisanApplication> findByStatus(ApplicationStatus status);
    List<ArtisanApplication> findByAccountOrderBySubmittedDateDesc(Account account);
    boolean existsByAccountAndStatus(Account account, ApplicationStatus status);
    List<ArtisanApplication> findByAccount_AccountIdOrderBySubmittedDateDesc(UUID accountId);

}
