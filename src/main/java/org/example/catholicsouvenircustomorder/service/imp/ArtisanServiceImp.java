package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.request.UpdateArtisanProfileRequest;
import org.example.catholicsouvenircustomorder.dto.response.account.ArtisanResponseDTO;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.Artisan;
import org.example.catholicsouvenircustomorder.repository.ArtisanRepository;
import org.example.catholicsouvenircustomorder.service.ArtisanService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArtisanServiceImp implements ArtisanService {

    private final ArtisanRepository artisanRepository;

    @Override
    public Page<ArtisanResponseDTO> getAllArtisans(Pageable pageable) {
        Page<Artisan> artisans = artisanRepository.findAll(pageable);
        return artisans.map(this::mapToDTO);
    }

    @Override
    public ArtisanResponseDTO getArtisanById(UUID artisanId) {
        Artisan artisan = artisanRepository.findById(artisanId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thợ thủ công với ID: " + artisanId));
        return mapToDTO(artisan);
    }

    @Override
    public ArtisanResponseDTO getArtisanProfile(UUID artisanId) {
        Artisan artisan = artisanRepository.findById(artisanId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ thợ thủ công"));
        return mapToDTO(artisan);
    }

    @Override
    @Transactional
    public ArtisanResponseDTO updateArtisanProfile(UUID artisanId, UpdateArtisanProfileRequest request) {
        Artisan artisan = artisanRepository.findById(artisanId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ thợ thủ công"));

        if (request.getArtisanName() != null && !request.getArtisanName().trim().isEmpty()) {
            artisan.setArtisanName(request.getArtisanName());
        }
        
        // Update phone number in Account
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            artisan.getAccount().setPhone(request.getPhoneNumber());
        }

        if (request.getBio() != null) {
            artisan.setBio(request.getBio());
        }

        if (request.getExperienceYears() != null) {
            artisan.setExperience_year(request.getExperienceYears());
        }

        if (request.getPortfolioUrl() != null) {
            artisan.setPortfolioUrl(request.getPortfolioUrl());
        }

        if (request.getSpecialization() != null && !request.getSpecialization().trim().isEmpty()) {
            artisan.setSpecialization(request.getSpecialization());
        }

        Artisan updatedArtisan = artisanRepository.save(artisan);
        return mapToDTO(updatedArtisan);
    }

    private ArtisanResponseDTO mapToDTO(Artisan artisan) {
        return ArtisanResponseDTO.builder()
                .artisanId(artisan.getArtisanUuid())
                .artisanName(artisan.getArtisanName())
                .phoneNumber(artisan.getAccount().getPhone())  // ← PHONE NUMBER HERE
                .profileImageUrl(artisan.getAccount().getAvt_url())
                .bio(artisan.getBio())
                .specialization(artisan.getSpecialization())
                .experienceYears(artisan.getExperience_year())
                .portfolioUrl(artisan.getPortfolioUrl())
                .build();
    }

    @Override
    @Transactional
    public void blacklistArtisan(UUID artisanId) {
        Artisan artisan = artisanRepository.findById(artisanId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thợ thủ công với ID: " + artisanId));
        artisan.setBlacklisted(true);
        artisanRepository.save(artisan);
    }

    @Override
    @Transactional
    public void unblacklistArtisan(UUID artisanId) {
        Artisan artisan = artisanRepository.findById(artisanId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thợ thủ công với ID: " + artisanId));
        artisan.setBlacklisted(false);
        artisanRepository.save(artisan);
    }

    @Override
    public boolean isArtisanBlacklisted(UUID artisanId) {
        Artisan artisan = artisanRepository.findById(artisanId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thợ thủ công với ID: " + artisanId));
        return artisan.isBlacklisted();
    }
    

}
