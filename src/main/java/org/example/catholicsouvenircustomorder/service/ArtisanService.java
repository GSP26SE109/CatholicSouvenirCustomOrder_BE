package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.UpdateArtisanProfileRequest;
import org.example.catholicsouvenircustomorder.dto.response.account.ArtisanResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ArtisanService {
    Page<ArtisanResponseDTO> getAllArtisans(Pageable pageable);
    ArtisanResponseDTO getArtisanById(UUID artisanId);
    ArtisanResponseDTO getArtisanProfile(UUID artisanId);
    ArtisanResponseDTO updateArtisanProfile(UUID artisanId, UpdateArtisanProfileRequest request);
}
