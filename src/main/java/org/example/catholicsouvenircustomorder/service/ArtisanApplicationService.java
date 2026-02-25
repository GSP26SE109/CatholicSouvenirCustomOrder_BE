package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.ArtisanApplicationRequest;
import org.example.catholicsouvenircustomorder.dto.request.RegisterArtisanRequest;
import org.example.catholicsouvenircustomorder.dto.request.ReviewApplicationRequest;
import org.example.catholicsouvenircustomorder.dto.response.ArtisanApplicationResponse;
import org.example.catholicsouvenircustomorder.model.Account;

import java.util.List;
import java.util.UUID;

public interface ArtisanApplicationService {
    // Đăng ký artisan cho người mới
    ArtisanApplicationResponse registerAsArtisan(RegisterArtisanRequest request);
    
    // Customer nộp đơn artisan
    ArtisanApplicationResponse submitApplication(ArtisanApplicationRequest request, UUID accountId);
    
    // Admin phê duyệt/từ chối
    ArtisanApplicationResponse reviewApplication(UUID applicationId, ReviewApplicationRequest request, Account admin);
    
    // Lấy danh sách đơn chờ duyệt
    List<ArtisanApplicationResponse> getPendingApplications();
    
    // Lấy đơn của user
    List<ArtisanApplicationResponse> getMyApplications(UUID accountId);
    
    // Lấy chi tiết đơn
    ArtisanApplicationResponse getApplicationById(UUID applicationId);
}
