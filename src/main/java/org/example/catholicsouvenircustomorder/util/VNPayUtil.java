package org.example.catholicsouvenircustomorder.util;

import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.config.VNPayConfig;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Component
public class VNPayUtil {

    private static final String HMAC_SHA512 = "HmacSHA512";

    private final VNPayConfig vnPayConfig;

    public VNPayUtil(VNPayConfig vnPayConfig) {
        this.vnPayConfig = vnPayConfig;
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

        // CRITICAL: vnp_IpnUrl KHÔNG được gửi trong payment URL!
        // IPN URL phải được đăng ký trước trong VNPay merchant portal
        // Nếu gửi vnp_IpnUrl trong params → VNPay sẽ tính hash sai → Code 70
        // VNPay sẽ dùng IPN URL đã đăng ký để callback
        log.info("IPN URL (registered in VNPay portal): {}", vnPayConfig.getIpnUrl());

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
}
