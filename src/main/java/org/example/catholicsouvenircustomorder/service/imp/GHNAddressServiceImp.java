package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.catholicsouvenircustomorder.config.GHNConfig;
import org.example.catholicsouvenircustomorder.exception.BadRequestException;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.service.GHNAddressService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GHNAddressServiceImp implements GHNAddressService {

    private final GHNConfig ghnConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    @Cacheable(value = "ghn_provinces", unless = "#result == null")
    public List<Map<String, Object>> getProvinces() {
        try {
            String url = ghnConfig.getApiUrl() + "/shiip/public-api/master-data/province";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Token", ghnConfig.getToken());

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("data")) {
                throw new BadRequestException("GHN API không trả về dữ liệu");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> provinces = (List<Map<String, Object>>) body.get("data");
            
            return provinces.stream()
                .map(p -> Map.of(
                    "provinceId", p.get("ProvinceID"),
                    "provinceName", p.get("ProvinceName"),
                    "nameExtension", p.getOrDefault("NameExtension", List.of())
                ))
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting provinces from GHN: {}", e.getMessage(), e);
            throw new BadRequestException("Không thể lấy danh sách tỉnh/thành phố");
        }
    }

    @Override
    @Cacheable(value = "ghn_districts", key = "#provinceId", unless = "#result == null")
    public List<Map<String, Object>> getDistricts(Integer provinceId) {
        try {
            String url = ghnConfig.getApiUrl() + "/shiip/public-api/master-data/district";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Token", ghnConfig.getToken());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("province_id", provinceId);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("data")) {
                throw new BadRequestException("GHN API không trả về dữ liệu");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> districts = (List<Map<String, Object>>) body.get("data");
            
            return districts.stream()
                .map(d -> Map.of(
                    "districtId", d.get("DistrictID"),
                    "districtName", d.get("DistrictName"),
                    "provinceId", d.get("ProvinceID"),
                    "nameExtension", d.getOrDefault("NameExtension", List.of())
                ))
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting districts from GHN: {}", e.getMessage(), e);
            throw new BadRequestException("Không thể lấy danh sách quận/huyện");
        }
    }

    @Override
    @Cacheable(value = "ghn_wards", key = "#districtId", unless = "#result == null")
    public List<Map<String, Object>> getWards(Integer districtId) {
        try {
            String url = ghnConfig.getApiUrl() + "/shiip/public-api/master-data/ward";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Token", ghnConfig.getToken());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("district_id", districtId);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("data")) {
                throw new BadRequestException("GHN API không trả về dữ liệu");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> wards = (List<Map<String, Object>>) body.get("data");
            
            return wards.stream()
                .map(w -> Map.of(
                    "wardCode", w.get("WardCode"),
                    "wardName", w.get("WardName"),
                    "districtId", w.get("DistrictID"),
                    "nameExtension", w.getOrDefault("NameExtension", List.of())
                ))
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting wards from GHN: {}", e.getMessage(), e);
            throw new BadRequestException("Không thể lấy danh sách phường/xã");
        }
    }

    @Override
    public Map<String, Object> searchDistrict(Integer provinceId, String districtName) {
        List<Map<String, Object>> districts = getDistricts(provinceId);
        
        String normalizedSearch = normalizeVietnamese(districtName.toLowerCase());
        
        return districts.stream()
            .filter(d -> {
                String name = normalizeVietnamese(d.get("districtName").toString().toLowerCase());
                return name.contains(normalizedSearch) || normalizedSearch.contains(name);
            })
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException(
                "Không tìm thấy quận/huyện: " + districtName));
    }

    @Override
    public Map<String, Object> searchWard(Integer districtId, String wardName) {
        List<Map<String, Object>> wards = getWards(districtId);
        
        String normalizedSearch = normalizeVietnamese(wardName.toLowerCase());
        
        return wards.stream()
            .filter(w -> {
                String name = normalizeVietnamese(w.get("wardName").toString().toLowerCase());
                return name.contains(normalizedSearch) || normalizedSearch.contains(name);
            })
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException(
                "Không tìm thấy phường/xã: " + wardName));
    }

    /**
     * Normalize Vietnamese text for better search
     */
    private String normalizeVietnamese(String text) {
        return text
            .replaceAll("quận|quan|q\\.|q ", "")
            .replaceAll("phường|phuong|p\\.|p ", "")
            .replaceAll("xã|xa|x\\.|x ", "")
            .replaceAll("thị trấn|thi tran|tt\\.|tt ", "")
            .replaceAll("\\s+", " ")
            .trim();
    }
}
