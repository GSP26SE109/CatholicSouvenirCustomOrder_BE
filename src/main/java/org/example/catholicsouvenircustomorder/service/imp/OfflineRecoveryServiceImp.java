package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.AccountRepository;
import org.example.catholicsouvenircustomorder.repository.NotificationRepository;
import org.example.catholicsouvenircustomorder.service.OfflineRecoveryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OfflineRecoveryServiceImp implements OfflineRecoveryService {
    
    private final NotificationRepository notificationRepository;
    private final AccountRepository accountRepository;
    
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createRecoveryTask(CustomOrder customOrder, BigDecimal refundAmount, String reason) {
        Artisan artisan = customOrder.getArtisan();
        Account artisanAccount = artisan.getAccount();
        
        // Extract Artisan info
        String artisanName = artisanAccount.getFullName();
        String artisanEmail = artisanAccount.getEmail();
        String artisanPhone = artisanAccount.getPhone();
        
        // Get all admin accounts
        List<Account> admins = accountRepository.findByRole_Name("ADMIN");
        
        // Create high-priority notification for each admin
        for (Account admin : admins) {
            Notification notification = new Notification();
            notification.setRecipient(admin);
            notification.setType(NotificationType.OFFLINE_RECOVERY_REQUIRED);
            notification.setTitle("Offline Recovery Required");
            notification.setMessage(String.format(
                "Artisan %s cannot refund %s VND for order %s. Contact: %s, Phone: %s. Reason: %s",
                artisanName,
                refundAmount,
                customOrder.getCustomOrderId(),
                artisanEmail,
                artisanPhone,
                reason
            ));
            notification.setRelatedEntityId(customOrder.getCustomOrderId());
            notification.setRelatedEntityType(RelatedEntityType.CUSTOM_ORDER);
            notification.setActionType(NotificationAction.REVIEW_RECOVERY);
            notification.setActionRequired(true);
            notification.setActionCompleted(false);
            notification.setPriority(NotificationPriority.HIGH);
            
            // Store metadata as JSON-like string
            String metadata = String.format(
                "artisanId=%s;artisanName=%s;artisanEmail=%s;artisanPhone=%s;refundAmount=%s;orderId=%s;reason=%s",
                artisan.getArtisanUuid(),
                artisanName,
                artisanEmail,
                artisanPhone,
                refundAmount,
                customOrder.getCustomOrderId(),
                reason
            );
            notification.setMetadata(metadata);
            
            notificationRepository.save(notification);
        }
        
        // Log recovery task with all details
        log.error("Offline recovery required: artisan={}, email={}, phone={}, amount={}, orderId={}, reason={}",
            artisanName,
            artisanEmail,
            artisanPhone,
            refundAmount,
            customOrder.getCustomOrderId(),
            reason);
    }
}
