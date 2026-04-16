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
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(400, ex.getMessage()));
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.error(500, "Lỗi hệ thống: " + ex.getMessage()));
    }
}
