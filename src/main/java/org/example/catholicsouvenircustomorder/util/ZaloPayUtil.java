package org.example.catholicsouvenircustomorder.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Component
public class ZaloPayUtil {
    
    private static final String HMAC_SHA256 = "HmacSHA256";
    
    public String generateMac(String data, String key) throws Exception {
        Mac hmac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        hmac.init(secretKey);
        byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }
    
    public boolean verifyCallback(Map<String, String> params, String key) {
        try {
            String receivedMac = params.get("mac");
            params.remove("mac");
            
            String data = buildDataString(params);
            String calculatedMac = generateMac(data, key);
            
            return calculatedMac.equals(receivedMac);
        } catch (Exception e) {
            log.error("Error verifying ZaloPay callback", e);
            return false;
        }
    }
    
    public String getCurrentTimestamp() {
        return String.valueOf(System.currentTimeMillis());
    }
    
    public String generateAppTransId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
        String date = sdf.format(new Date());
        return date + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    private String buildDataString(Map<String, String> params) {
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        
        StringBuilder data = new StringBuilder();
        for (String key : keys) {
            if (data.length() > 0) {
                data.append("&");
            }
            data.append(key).append("=").append(params.get(key));
        }
        return data.toString();
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
