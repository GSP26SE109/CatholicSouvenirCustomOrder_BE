package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.response.account.ArtisanResponseDTO;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.Artisan;
import org.example.catholicsouvenircustomorder.repository.ArtisanRepository;
import org.example.catholicsouvenircustomorder.service.ArtisanService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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

    private ArtisanResponseDTO mapToDTO(Artisan artisan) {
        return ArtisanResponseDTO.builder()
                .artisanId(artisan.getArtisanUuid())
                .artisanName(artisan.getArtisanName())
                .profileImageUrl(artisan.getAccount().getAvt_url())
                .bio(artisan.getBio())
                .specialization(artisan.getSpecialization())
                .experienceYears(artisan.getExperience_year())
                .portfolioUrl(artisan.getPortfolioUrl())
                .build();
    }
}
