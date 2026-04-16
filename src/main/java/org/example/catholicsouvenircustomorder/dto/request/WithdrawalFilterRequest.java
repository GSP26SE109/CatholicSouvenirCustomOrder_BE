package org.example.catholicsouvenircustomorder.dto.request;

import lombok.Data;
import org.example.catholicsouvenircustomorder.model.WithdrawalStatus;

import java.time.LocalDate;

@Data
public class WithdrawalFilterRequest {
    
    private WithdrawalStatus status;
    
    private String artisanName;
    
    private LocalDate fromDate;
    
    private LocalDate toDate;
    
    private int page = 0;
    
    private int size = 20;
}
