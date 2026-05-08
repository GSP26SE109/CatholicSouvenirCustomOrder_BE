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
        Account customerAccount = customOrder.getRequest().getCustomer(); // Get customer from request
        Wallet artisanWallet = artisan.getWallet();
        
        // Extract Artisan info
        String artisanName = artisanAccount.getFullName();
        String artisanEmail = artisanAccount.getEmail();
        String artisanPhone = artisanAccount.getPhone();
        
        // Extract Customer info
        String customerName = customerAccount.getFullName();
        String customerEmail = customerAccount.getEmail();
        String customerPhone = customerAccount.getPhone();
        
        // Get wallet balances
        BigDecimal availableBalance = artisanWallet.getAvailableBalance();
        BigDecimal lockedBalance = artisanWallet.getLockedBalance() != null ? artisanWallet.getLockedBalance() : BigDecimal.ZERO;
        
        // Get all admin accounts
        List<Account> admins = accountRepository.findByRole_Name("ADMIN");
        
        // Create high-priority notification for each admin
        for (Account admin : admins) {
            Notification notification = new Notification();
            notification.setRecipient(admin);
            notification.setType(NotificationType.OFFLINE_RECOVERY_REQUIRED);
            notification.setTitle("Offline Recovery Required");
            notification.setMessage(String.format(
                "Artisan %s cannot refund %s VND for order %s. Customer: %s (%s). Contact Artisan: %s, Phone: %s. Available: %s VND, Locked: %s VND. Reason: %s",
                artisanName,
                refundAmount,
                customOrder.getCustomOrderId(),
                customerName,
                customerEmail,
                artisanEmail,
                artisanPhone,
                availableBalance,
                lockedBalance,
                reason
            ));
            notification.setRelatedEntityId(customOrder.getCustomOrderId());
            notification.setRelatedEntityType(RelatedEntityType.CUSTOM_ORDER);
            notification.setActionType(NotificationAction.REVIEW_RECOVERY);
            notification.setActionRequired(true);
            notification.setActionCompleted(false);
            notification.setPriority(NotificationPriority.HIGH);
            
            // Store metadata with both artisan and customer info
            String metadata = String.format(
                "artisanId=%s;artisanName=%s;artisanEmail=%s;artisanPhone=%s;customerId=%s;customerName=%s;customerEmail=%s;customerPhone=%s;refundAmount=%s;availableBalance=%s;lockedBalance=%s;orderId=%s;reason=%s",
                artisan.getArtisanUuid(),
                artisanName,
                artisanEmail,
                artisanPhone,
                customerAccount.getAccountId(),
                customerName,
                customerEmail,
                customerPhone,
                refundAmount,
                availableBalance,
                lockedBalance,
                customOrder.getCustomOrderId(),
                reason
            );
            notification.setMetadata(metadata);
            
            notificationRepository.save(notification);
        }
        
        // Log recovery task with all details
        log.error("Offline recovery required: artisan={}, email={}, phone={}, customer={}, customerEmail={}, amount={}, available={}, locked={}, orderId={}, reason={}",
            artisanName,
            artisanEmail,
            artisanPhone,
            customerName,
            customerEmail,
            refundAmount,
            availableBalance,
            lockedBalance,
            customOrder.getCustomOrderId(),
            reason);
    }
}
