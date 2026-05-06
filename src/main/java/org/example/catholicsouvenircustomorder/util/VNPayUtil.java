package org.example.catholicsouvenircustomorder.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.config.VNPayConfig;
import org.example.catholicsouvenircustomorder.dto.response.VNPayRefundResponse;
import org.example.catholicsouvenircustomorder.dto.response.VNPayRefundStatusResponse;
import org.example.catholicsouvenircustomorder.exception.VNPayException;
import org.example.catholicsouvenircustomorder.exception.VNPayNetworkException;
import org.example.catholicsouvenircustomorder.exception.VNPayTimeoutException;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
public class VNPayUtil {

    private static final String HMAC_SHA512 = "HmacSHA512";
    private static final DateTimeFormatter VNPAY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VNPayConfig vnPayConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public VNPayUtil(VNPayConfig vnPayConfig) {
        this.vnPayConfig = vnPayConfig;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public String createPaymentUrl(String transactionId, BigDecimal amount, String description, String customerEmail) throws Exception {
        return createPaymentUrl(transactionId, amount, description, customerEmail, null);
    }

    public String createPaymentUrl(String transactionId, BigDecimal amount, String description, String customerEmail, String returnUrl) throws Exception {
        log.info("=== Creating VNPay Payment URL ===");
        log.info("TMN Code: {}", vnPayConfig.getTmnCode());
        log.info("Hash Secret Length: {}", vnPayConfig.getHashSecret().length());
        log.info("Version: {}", vnPayConfig.getVersion());
        log.info("Command: {}", vnPayConfig.getCommand());

        // Use TreeMap to ensure alphabetical order
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", vnPayConfig.getVersion());
        params.put("vnp_Command", vnPayConfig.getCommand());
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_Amount", String.valueOf(amount.multiply(new BigDecimal(100)).longValue())); // VNPay uses smallest unit
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", transactionId);
        params.put("vnp_OrderInfo", description);
        params.put("vnp_OrderType", vnPayConfig.getOrderType());
        params.put("vnp_Locale", "vn");

        // Return URL: where user is redirected (FE web or mobile deep link)
        // VNPay will redirect user here with payment params in URL
        params.put("vnp_ReturnUrl", returnUrl != null ? returnUrl : vnPayConfig.getReturnUrl());


        params.put("vnp_IpAddr", "127.0.0.1");
        params.put("vnp_CreateDate", getVNPayDate());
        params.put("vnp_ExpireDate", getExpireDate(15));

        // Generate secure hash
        String secureHash = generateSecureHash(params, vnPayConfig.getHashSecret());
        params.put("vnp_SecureHash", secureHash);

        // Build URL
        String queryUrl = buildQueryUrl(params);
        String fullUrl = vnPayConfig.getUrl() + "?" + queryUrl;
        log.info("Payment URL created successfully");
        log.info("Return URL: {}", params.get("vnp_ReturnUrl"));
        log.info("IPN URL: {}", params.get("vnp_IpnUrl"));
        log.info("================================");
        return fullUrl;
    }

    public String generateSecureHash(Map<String, String> params, String secretKey) throws Exception {
        String data = buildHashData(params);
        log.info("=== VNPay Hash Debug ===");
        log.info("Hash data string: {}", data);
        log.info("Secret key length: {}", secretKey.length());
        log.info("Secret key (first 10 chars): {}...", secretKey.substring(0, Math.min(10, secretKey.length())));
        String hash = hmacSHA512(data, secretKey);
        log.info("Generated secure hash: {}", hash);
        log.info("======================");
        return hash;
    }

    public boolean verifySecureHash(Map<String, String> params, String secretKey) {
        try {
            String receivedHash = params.get("vnp_SecureHash");
            
            // CRITICAL: Tạo copy để không mutate map gốc
            Map<String, String> paramsCopy = new TreeMap<>(params);
            paramsCopy.remove("vnp_SecureHash");
            paramsCopy.remove("vnp_SecureHashType");

            String calculatedHash = generateSecureHash(paramsCopy, secretKey);
            
            log.info("=== VNPay Signature Verification ===");
            log.info("Received hash: {}", receivedHash);
            log.info("Calculated hash: {}", calculatedHash);
            log.info("Match: {}", calculatedHash.equals(receivedHash));
            log.info("===================================");
            
            return calculatedHash.equals(receivedHash);
        } catch (Exception e) {
            log.error("Error verifying VNPay secure hash", e);
            return false;
        }
    }

    public String getVNPayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return sdf.format(new Date());
    }

    public String getExpireDate(int minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, minutes);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return sdf.format(calendar.getTime());
    }

    public String buildQueryUrl(Map<String, String> params) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        StringBuilder query = new StringBuilder();
        for (String key : keys) {
            String value = params.get(key);
            if (value != null && !value.isEmpty()) {
                if (query.length() > 0) {
                    query.append("&");
                }
                // Key KHÔNG encode, value encode và replace + thành %20
                query.append(key)
                        .append("=")
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        }
        return query.toString();
    }

    private String buildHashData(Map<String, String> params) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        StringBuilder hashData = new StringBuilder();
        for (String key : keys) {
            // Bỏ qua vnp_SecureHash và vnp_SecureHashType
            if ("vnp_SecureHash".equals(key) || "vnp_SecureHashType".equals(key)) {
                continue;
            }
            
            String value = params.get(key);
            if (value != null && !value.isEmpty()) {
                if (hashData.length() > 0) {
                    hashData.append("&");
                }
                // CRITICAL: Hash data phải dùng CÙNG encoding với query URL
                // Theo VNPay: encode và replace + thành %20
                hashData.append(key)
                        .append("=")
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        }
        return hashData.toString();
    }

    private String hmacSHA512(String data, String key) throws Exception {
        Mac hmac = Mac.getInstance(HMAC_SHA512);
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA512);
        hmac.init(secretKey);
        byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Create a refund request to VNPay
     * Requirements: 12.2 - Add @Retryable annotation with exponential backoff
     * 
     * @param originalTransactionId Original vnp_TransactionNo from the payment
     * @param refundAmount Amount to refund in VND
     * @param refundReason Reason for the refund
     * @return VNPayRefundResponse containing refund details
     * @throws VNPayException if refund request fails
     * @throws VNPayTimeoutException if request times out (retryable)
     * @throws VNPayNetworkException if network error occurs (retryable)
     */
    @Retryable(
        value = {VNPayTimeoutException.class, VNPayNetworkException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public VNPayRefundResponse createRefundRequest(
            String originalTransactionId,
            BigDecimal refundAmount,
            String refundReason
    ) throws VNPayException, VNPayTimeoutException, VNPayNetworkException {
        log.info("=== Creating VNPay Refund Request (Attempt) ===");
        log.info("Original Transaction ID: {}", originalTransactionId);
        log.info("Refund Amount: {} VND", refundAmount);
        log.info("Refund Reason: {}", refundReason);

        // Generate unique request ID
        String requestId = UUID.randomUUID().toString();
        String createDate = getVNPayDate();
        
        // Build refund request parameters
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_RequestId", requestId);
        params.put("vnp_Version", vnPayConfig.getVersion());
        params.put("vnp_Command", "refund");
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_TransactionType", "03"); // 03 = Partial refund (allows refunding less than original amount)
        params.put("vnp_TxnRef", originalTransactionId);
        params.put("vnp_Amount", String.valueOf(refundAmount.multiply(new BigDecimal(100)).longValue()));
        params.put("vnp_OrderInfo", refundReason);
        params.put("vnp_TransactionNo", originalTransactionId);
        params.put("vnp_TransactionDate", createDate);
        params.put("vnp_CreateBy", "system");
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_IpAddr", "127.0.0.1");

        // Generate secure hash for refund
        try {
            String secureHash = generateRefundHash(params, vnPayConfig.getHashSecret());
            params.put("vnp_SecureHash", secureHash);

            log.info("Refund request parameters prepared");
            log.info("Request ID: {}", requestId);
            log.info("Secure Hash: {}", secureHash);
        } catch (Exception e) {
            log.error("Failed to generate secure hash for refund request", e);
            throw new VNPayException("97", "Lỗi tạo chữ ký bảo mật", e);
        }

        // Send POST request to VNPay API
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(params, headers);
            
            log.info("Sending refund request to VNPay API: {}", vnPayConfig.getApiUrl());
            ResponseEntity<String> response = restTemplate.postForEntity(
                    vnPayConfig.getApiUrl(),
                    request,
                    String.class
            );

            log.info("VNPay API Response Status: {}", response.getStatusCode());
            log.info("VNPay API Response Body: {}", response.getBody());

            // Parse response
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            String responseCode = responseJson.get("vnp_ResponseCode").asText();
            String message = VNPayErrorMapper.mapErrorCode(responseCode);

            VNPayRefundResponse refundResponse = VNPayRefundResponse.builder()
                    .vnpayRefundId(requestId)
                    .vnpayTransactionNo(responseJson.has("vnp_TransactionNo") 
                            ? responseJson.get("vnp_TransactionNo").asText() 
                            : originalTransactionId)
                    .responseCode(responseCode)
                    .message(message)
                    .refundAmount(refundAmount)
                    .refundDate(LocalDateTime.now())
                    .build();

            if (VNPayErrorMapper.isSuccess(responseCode)) {
                log.info("Refund request successful");
            } else {
                log.error("Refund request failed with code: {} - {}", responseCode, message);
                
                // Check if this is a retryable error
                if (VNPayErrorMapper.isRetryable(responseCode)) {
                    if ("06".equals(responseCode)) {
                        throw new VNPayTimeoutException("VNPay đang xử lý giao dịch, thử lại sau");
                    } else {
                        throw new VNPayNetworkException("Lỗi mạng VNPay: " + message);
                    }
                } else {
                    // Non-retryable error
                    throw new VNPayException(responseCode, message);
                }
            }

            log.info("=====================================");
            return refundResponse;

        } catch (ResourceAccessException e) {
            // Network timeout or connection issues - retryable
            log.error("VNPay API network error (retryable)", e);
            if (e.getCause() instanceof SocketTimeoutException) {
                throw new VNPayTimeoutException("VNPay API timeout", e);
            } else {
                throw new VNPayNetworkException("VNPay API network error", e);
            }
        } catch (RestClientException e) {
            // Other REST client errors - may be retryable
            log.error("VNPay API REST client error", e);
            throw new VNPayNetworkException("VNPay API client error: " + e.getMessage(), e);
        } catch (VNPayTimeoutException | VNPayNetworkException e) {
            // Re-throw our timeout and network exceptions
            throw e;
        } catch (VNPayException e) {
            // Re-throw other VNPay exceptions
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error calling VNPay refund API", e);
            log.error("Exception type: {}", e.getClass().getName());
            log.error("Exception message: {}", e.getMessage());
            throw new VNPayException("99", "Lỗi không xác định khi gọi VNPay API: " + e.getMessage(), e);
        }
    }

    /**
     * Generate secure hash specifically for refund requests
     * 
     * @param params Refund request parameters
     * @param secretKey VNPay secret key
     * @return HMAC SHA512 hash
     * @throws Exception if hash generation fails
     */
    public String generateRefundHash(Map<String, String> params, String secretKey) throws Exception {
        log.info("=== Generating Refund Hash ===");
        
        // Create a copy without vnp_SecureHash
        Map<String, String> paramsCopy = new TreeMap<>(params);
        paramsCopy.remove("vnp_SecureHash");
        paramsCopy.remove("vnp_SecureHashType");
        
        String hashData = buildHashData(paramsCopy);
        log.info("Refund hash data: {}", hashData);
        log.info("Secret key length: {}", secretKey.length());
        
        String hash = hmacSHA512(hashData, secretKey);
        log.info("Generated refund hash: {}", hash);
        log.info("==============================");
        
        return hash;
    }

    /**
     * Query the status of a refund transaction
     * Requirements: 12.2 - Add @Retryable annotation with exponential backoff
     * 
     * @param refundId The refund request ID (vnp_RequestId)
     * @return VNPayRefundStatusResponse containing current refund status
     * @throws VNPayException if query fails
     * @throws VNPayTimeoutException if request times out (retryable)
     * @throws VNPayNetworkException if network error occurs (retryable)
     */
    @Retryable(
        value = {VNPayTimeoutException.class, VNPayNetworkException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public VNPayRefundStatusResponse queryRefundStatus(String refundId) 
            throws VNPayException, VNPayTimeoutException, VNPayNetworkException {
        log.info("=== Querying VNPay Refund Status (Attempt) ===");
        log.info("Refund ID: {}", refundId);

        String requestId = UUID.randomUUID().toString();
        String createDate = getVNPayDate();

        // Build query parameters
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_RequestId", requestId);
        params.put("vnp_Version", vnPayConfig.getVersion());
        params.put("vnp_Command", "querydr"); // Query transaction command
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_TxnRef", refundId);
        params.put("vnp_OrderInfo", "Query refund status");
        params.put("vnp_TransactionDate", createDate);
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_IpAddr", "127.0.0.1");

        // Generate secure hash
        try {
            String secureHash = generateRefundHash(params, vnPayConfig.getHashSecret());
            params.put("vnp_SecureHash", secureHash);

            log.info("Query parameters prepared");
            log.info("Request ID: {}", requestId);
        } catch (Exception e) {
            log.error("Failed to generate secure hash for status query", e);
            throw new VNPayException("97", "Lỗi tạo chữ ký bảo mật", e);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(params, headers);
            
            log.info("Sending status query to VNPay API: {}", vnPayConfig.getApiUrl());
            ResponseEntity<String> response = restTemplate.postForEntity(
                    vnPayConfig.getApiUrl(),
                    request,
                    String.class
            );

            log.info("VNPay API Response Status: {}", response.getStatusCode());
            log.info("VNPay API Response Body: {}", response.getBody());

            // Parse response
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            String responseCode = responseJson.get("vnp_ResponseCode").asText();
            String transactionStatus = responseJson.has("vnp_TransactionStatus") 
                    ? responseJson.get("vnp_TransactionStatus").asText() 
                    : "unknown";

            VNPayRefundStatusResponse statusResponse = VNPayRefundStatusResponse.builder()
                    .vnpayRefundId(refundId)
                    .vnpayTransactionNo(responseJson.has("vnp_TransactionNo") 
                            ? responseJson.get("vnp_TransactionNo").asText() 
                            : null)
                    .statusCode(transactionStatus)
                    .statusMessage(VNPayErrorMapper.mapErrorCode(transactionStatus))
                    .refundAmount(responseJson.has("vnp_Amount") 
                            ? new BigDecimal(responseJson.get("vnp_Amount").asLong()).divide(new BigDecimal(100))
                            : null)
                    .originalTxnRef(responseJson.has("vnp_TxnRef") 
                            ? responseJson.get("vnp_TxnRef").asText() 
                            : null)
                    .lastUpdated(LocalDateTime.now())
                    .responseCode(responseCode)
                    .build();

            // Check for errors in response
            if (!VNPayErrorMapper.isSuccess(responseCode)) {
                log.error("Status query failed with code: {} - {}", responseCode, VNPayErrorMapper.mapErrorCode(responseCode));
                
                // Check if this is a retryable error
                if (VNPayErrorMapper.isRetryable(responseCode)) {
                    if ("06".equals(responseCode)) {
                        throw new VNPayTimeoutException("VNPay đang xử lý truy vấn, thử lại sau");
                    } else {
                        throw new VNPayNetworkException("Lỗi mạng VNPay: " + VNPayErrorMapper.mapErrorCode(responseCode));
                    }
                } else {
                    // Non-retryable error
                    throw new VNPayException(responseCode, VNPayErrorMapper.mapErrorCode(responseCode));
                }
            }

            log.info("Refund status query completed");
            log.info("Status Code: {}", transactionStatus);
            log.info("Response Code: {}", responseCode);
            log.info("====================================");

            return statusResponse;

        } catch (ResourceAccessException e) {
            // Network timeout or connection issues - retryable
            log.error("VNPay API network error during status query (retryable)", e);
            if (e.getCause() instanceof SocketTimeoutException) {
                throw new VNPayTimeoutException("VNPay API timeout during status query", e);
            } else {
                throw new VNPayNetworkException("VNPay API network error during status query", e);
            }
        } catch (RestClientException e) {
            // Other REST client errors - may be retryable
            log.error("VNPay API REST client error during status query", e);
            throw new VNPayNetworkException("VNPay API client error during status query: " + e.getMessage(), e);
        } catch (VNPayTimeoutException | VNPayNetworkException e) {
            // Re-throw our timeout and network exceptions
            throw e;
        } catch (VNPayException e) {
            // Re-throw other VNPay exceptions
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error querying VNPay refund status", e);
            log.error("Exception type: {}", e.getClass().getName());
            log.error("Exception message: {}", e.getMessage());
            throw new VNPayException("99", "Lỗi không xác định khi truy vấn trạng thái VNPay: " + e.getMessage(), e);
        }
    }
}
