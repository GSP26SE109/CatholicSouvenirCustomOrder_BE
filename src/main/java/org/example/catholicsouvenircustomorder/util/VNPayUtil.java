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

    public String getHashSecret() {
        return vnPayConfig.getHashSecret();
    }

    public String createPaymentUrl(String transactionId, BigDecimal amount,
                                   String description, String customerEmail,
                                   String returnUrl) throws Exception {
        // ✅ Dùng TreeMap để tự sort theo key alphabet
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version",   vnPayConfig.getVersion());
        params.put("vnp_Command",   vnPayConfig.getCommand());
        params.put("vnp_TmnCode",   vnPayConfig.getTmnCode());
        params.put("vnp_Amount",    String.valueOf(amount.multiply(new BigDecimal(100)).longValue()));
        params.put("vnp_CurrCode",  "VND");
        params.put("vnp_TxnRef",    transactionId);
        params.put("vnp_OrderInfo", description);
        params.put("vnp_OrderType", vnPayConfig.getOrderType());
        params.put("vnp_Locale",    "vn");
        params.put("vnp_ReturnUrl", returnUrl != null ? returnUrl : vnPayConfig.getReturnUrl());
        params.put("vnp_IpAddr",    "127.0.0.1");
        params.put("vnp_CreateDate", getVNPayDate());
        params.put("vnp_ExpireDate", getExpireDate(15));

        // ✅ Thêm IPN URL TRƯỚC khi tính hash
        if (vnPayConfig.getIpnUrl() != null && !vnPayConfig.getIpnUrl().isEmpty()) {
            params.put("vnp_IpnUrl", vnPayConfig.getIpnUrl());
        }

        // ✅ Tính hash với TẤT CẢ params (kể cả vnp_IpnUrl)
        String secureHash = generateSecureHash(params, vnPayConfig.getHashSecret());

        // ✅ Thêm hash vào params SAU KHI tính xong
        params.put("vnp_SecureHash", secureHash);

        String queryUrl = buildQueryString(params);
        String fullUrl = vnPayConfig.getUrl() + "?" + queryUrl;

        log.info("Payment URL created. ReturnUrl={}, IpnUrl={}",
                params.get("vnp_ReturnUrl"), params.get("vnp_IpnUrl"));
        return fullUrl;
    }

    public String createPaymentUrl(String transactionId, BigDecimal amount,
                                   String description, String customerEmail) throws Exception {
        return createPaymentUrl(transactionId, amount, description, customerEmail, null);
    }

    /**
     * Tính HMAC-SHA512 hash cho tập params
     * - Loại bỏ vnp_SecureHash, vnp_SecureHashType
     * - GIỮ LẠI vnp_IpnUrl (có trong hash theo chuẩn VNPay)
     * - Sort key theo alphabet
     * - Encode value bằng UTF-8, space thành %20
     */
    public String generateSecureHash(Map<String, String> params, String secretKey) throws Exception {
        // Sort key
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        StringBuilder hashData = new StringBuilder();
        for (String key : keys) {
            // Bỏ qua vnp_SecureHash và vnp_SecureHashType
            if ("vnp_SecureHash".equals(key) || "vnp_SecureHashType".equals(key)) continue;

            String value = params.get(key);
            if (value != null && !value.isEmpty()) {
                if (hashData.length() > 0) hashData.append("&");
                // ✅ key KHÔNG encode, value encode UTF-8, space → %20
                hashData.append(key)
                        .append("=")
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8)
                                .replace("+", "%20"));
            }
        }

        log.info("Hash input string: {}", hashData);
        String hash = hmacSHA512(hashData.toString(), secretKey);
        log.info("Hash output: {}", hash);
        return hash;
    }

    /**
     * Verify hash từ VNPay callback
     * Luôn truyền vào COPY của params map
     */
    public boolean verifySecureHash(Map<String, String> params, String secretKey) {
        try {
            String receivedHash = params.get("vnp_SecureHash");
            if (receivedHash == null || receivedHash.isEmpty()) {
                log.error("vnp_SecureHash not found in callback params");
                return false;
            }

            // ✅ Tính lại hash với params đã bỏ vnp_SecureHash
            String calculatedHash = generateSecureHash(params, secretKey);

            // ✅ So sánh case-insensitive (VNPay đôi khi trả uppercase/lowercase)
            boolean valid = calculatedHash.equalsIgnoreCase(receivedHash);
            if (!valid) {
                log.error("Hash MISMATCH. Calculated={}, Received={}", calculatedHash, receivedHash);
            }
            return valid;
        } catch (Exception e) {
            log.error("Error verifying VNPay hash", e);
            return false;
        }
    }

    /**
     * Build query string cho URL (bao gồm vnp_SecureHash)
     * key KHÔNG encode, value encode UTF-8, space → %20
     */
    private String buildQueryString(Map<String, String> params) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        StringBuilder query = new StringBuilder();
        for (String key : keys) {
            String value = params.get(key);
            if (value != null && !value.isEmpty()) {
                if (query.length() > 0) query.append("&");
                query.append(key)
                        .append("=")
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8)
                                .replace("+", "%20"));
            }
        }
        return query.toString();
    }

    public String getVNPayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh")); // ✅ đúng timezone VN
        return sdf.format(new Date());
    }

    public String getExpireDate(int minutes) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        cal.add(Calendar.MINUTE, minutes);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        return sdf.format(cal.getTime());
    }

    private String hmacSHA512(String data, String key) throws Exception {
        Mac hmac = Mac.getInstance(HMAC_SHA512);
        SecretKeySpec secretKey = new SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8), HMAC_SHA512);
        hmac.init(secretKey);
        byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
