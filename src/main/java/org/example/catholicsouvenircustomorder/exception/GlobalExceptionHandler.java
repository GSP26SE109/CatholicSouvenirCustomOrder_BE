package org.example.catholicsouvenircustomorder.exception;

import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.badRequest()
                .body(BaseResponse.error(400, "Dữ liệu không hợp lệ: " + errorMessage));
    }

    @ExceptionHandler(InsertException.class)
    public ResponseEntity<BaseResponse> handleInsertException(InsertException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(400, ex.getMessage()));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<BaseResponse> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(BaseResponse.error(409, ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<BaseResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(BaseResponse.error(401, ex.getMessage()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<BaseResponse> handleInvalidToken(InvalidTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(BaseResponse.error(401, ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BaseResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(BaseResponse.error(400, ex.getMessage()));
    }
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<BaseResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BaseResponse.error(404, ex.getMessage()));
    }
    
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<BaseResponse> handleNotFoundException(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BaseResponse.error(404, ex.getMessage()));
    }
    
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<BaseResponse> handleBadRequestException(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(400, ex.getMessage()));
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<BaseResponse> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.badRequest()
                .body(BaseResponse.error(400, ex.getMessage()));
    }
    
    @ExceptionHandler(ZoneValidationException.class)
    public ResponseEntity<BaseResponse> handleZoneValidationException(ZoneValidationException ex) {
        return ResponseEntity.badRequest()
                .body(BaseResponse.error(400, ex.getMessage()));
    }
    
    @ExceptionHandler(ImageGenerationLimitExceededException.class)
    public ResponseEntity<BaseResponse> handleImageGenerationLimitExceeded(ImageGenerationLimitExceededException ex) {
        return ResponseEntity.badRequest()
                .body(BaseResponse.error(400, ex.getMessage()));
    }
    
    @ExceptionHandler(UnauthorizedTemplateAccessException.class)
    public ResponseEntity<BaseResponse> handleUnauthorizedTemplateAccess(UnauthorizedTemplateAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(BaseResponse.error(403, ex.getMessage()));
    }
    
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<BaseResponse> handleUnauthorizedException(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(BaseResponse.error(401, ex.getMessage()));
    }
    
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<BaseResponse> handleInsufficientBalanceException(InsufficientBalanceException ex) {
        // For cancellation-related insufficient balance, provide clear message about offline recovery
        String message = ex.getMessage();
        if (message.contains("refund") || message.contains("cancellation")) {
            message += " Vui lòng liên hệ bộ phận hỗ trợ để xử lý offline.";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(400, message));
    }
    
    @ExceptionHandler(CancellationException.class)
    public ResponseEntity<BaseResponse> handleCancellationException(CancellationException ex) {
        // Map error codes to appropriate HTTP status codes
        HttpStatus status = switch (ex.getErrorCode()) {
            case CancellationException.UNAUTHORIZED -> HttpStatus.FORBIDDEN;
            case CancellationException.ALREADY_CANCELLED,
                 CancellationException.INVALID_ORDER_STATUS,
                 CancellationException.NO_PAID_STAGES -> HttpStatus.CONFLICT;
            case CancellationException.STAGE_COMPLETED,
                 CancellationException.INVALID_REASON,
                 CancellationException.INVALID_INITIATOR,
                 CancellationException.INSUFFICIENT_BALANCE -> HttpStatus.BAD_REQUEST;
            case CancellationException.REFUND_FAILED -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        
        // Provide user-friendly error messages
        String userMessage = switch (ex.getErrorCode()) {
            case CancellationException.UNAUTHORIZED -> 
                "Bạn không có quyền hủy đơn hàng này.";
            case CancellationException.ALREADY_CANCELLED -> 
                "Đơn hàng đã được hủy trước đó.";
            case CancellationException.STAGE_COMPLETED -> 
                "Không thể hủy đơn hàng có giai đoạn đã hoàn thành.";
            case CancellationException.INVALID_REASON -> 
                "Lý do hủy đơn không hợp lệ. Vui lòng nhập ít nhất 20 ký tự.";
            case CancellationException.NO_PAID_STAGES -> 
                "Không có giai đoạn nào đã thanh toán để hoàn tiền.";
            case CancellationException.INVALID_ORDER_STATUS -> 
                "Trạng thái đơn hàng không cho phép hủy.";
            case CancellationException.INSUFFICIENT_BALANCE -> 
                ex.getMessage() + " Vui lòng liên hệ bộ phận hỗ trợ để xử lý offline.";
            case CancellationException.REFUND_FAILED -> 
                "Không thể xử lý hoàn tiền. Vui lòng thử lại sau.";
            default -> ex.getMessage();
        };
        
        return ResponseEntity.status(status)
                .body(BaseResponse.error(status.value(), userMessage));
    }
    
    @ExceptionHandler(PendingWithdrawalExistsException.class)
    public ResponseEntity<BaseResponse> handlePendingWithdrawalExistsException(PendingWithdrawalExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(BaseResponse.error(409, ex.getMessage()));
    }
    
    @ExceptionHandler(InvalidStatusException.class)
    public ResponseEntity<BaseResponse> handleInvalidStatusException(InvalidStatusException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(400, ex.getMessage()));
    }
    
    @ExceptionHandler(CommissionCalculationException.class)
    public ResponseEntity<BaseResponse> handleCommissionCalculationException(CommissionCalculationException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.error(500, ex.getMessage()));
    }

    
    @ExceptionHandler(RefundProcessingException.class)
    public ResponseEntity<BaseResponse> handleRefundProcessingException(RefundProcessingException ex) {
        // Refund processing errors are typically bad requests or service errors
        HttpStatus status = switch (ex.getErrorCode()) {
            case RefundProcessingException.PAYMENT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case RefundProcessingException.REFUND_ALREADY_PROCESSED,
                 RefundProcessingException.INVALID_REFUND_AMOUNT -> HttpStatus.BAD_REQUEST;
            case RefundProcessingException.VNPAY_TIMEOUT,
                 RefundProcessingException.VNPAY_NETWORK_ERROR,
                 RefundProcessingException.VNPAY_ERROR,
                 RefundProcessingException.PARTIAL_REFUND_FAILURE -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        
        // Include VNPay response code in error message if available
        String errorMessage = ex.getMessage();
        if (ex.getVnpayResponseCode() != null) {
            errorMessage += " (Mã lỗi VNPay: " + ex.getVnpayResponseCode() + ")";
        }
        
        return ResponseEntity.status(status)
                .body(BaseResponse.error(status.value(), errorMessage));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.error(500, "Lỗi hệ thống: " + ex.getMessage()));
    }
}
