package org.example.catholicsouvenircustomorder.service;

import java.util.List;
import java.util.Map;

public interface GHNAddressService {
    
    /**
     * Lấy danh sách tỉnh/thành phố
     */
    List<Map<String, Object>> getProvinces();
    
    /**
     * Lấy danh sách quận/huyện theo tỉnh
     */
    List<Map<String, Object>> getDistricts(Integer provinceId);
    
    /**
     * Lấy danh sách phường/xã theo quận
     */
    List<Map<String, Object>> getWards(Integer districtId);
    
    /**
     * Tìm district từ tên
     */
    Map<String, Object> searchDistrict(Integer provinceId, String districtName);
    
    /**
     * Tìm ward từ tên
     */
    Map<String, Object> searchWard(Integer districtId, String wardName);
}
