package org.example.catholicsouvenircustomorder.util;

import org.springframework.stereotype.Component;

/**
 * Utility class for bank account operations
 * Provides methods to mask sensitive bank account information
 */
@Component
public class BankAccountUtil {

    /**
     * Mask account number for display (show last 4 digits)
     * Used when displaying to users for security purposes
     * 
     * @param accountNumber The bank account number to mask
     * @return Masked account number showing only last 4 digits (e.g., "****1234")
     */
    public String mask(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
