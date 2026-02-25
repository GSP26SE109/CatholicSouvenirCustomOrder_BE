package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.request.ArtisanApplicationRequest;
import org.example.catholicsouvenircustomorder.dto.request.RegisterArtisanRequest;
import org.example.catholicsouvenircustomorder.dto.request.ReviewApplicationRequest;
import org.example.catholicsouvenircustomorder.dto.response.ArtisanApplicationResponse;
import org.example.catholicsouvenircustomorder.model.*;
import org.example.catholicsouvenircustomorder.repository.AccountRepository;
import org.example.catholicsouvenircustomorder.repository.ArtisanApplicationRepository;
import org.example.catholicsouvenircustomorder.repository.ArtisanRepository;
import org.example.catholicsouvenircustomorder.repository.RoleRepository;
import org.example.catholicsouvenircustomorder.service.ArtisanApplicationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArtisanApplicationServiceImp implements ArtisanApplicationService {

    private final ArtisanApplicationRepository applicationRepository;
    private final ArtisanRepository artisanRepository;
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public ArtisanApplicationResponse registerAsArtisan(RegisterArtisanRequest request) {

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp");
        }


        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng");
        }

        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Role CUSTOMER không tồn tại"));

        Account account = new Account();
        account.setFullName(request.getFirstName() + " " + request.getLastName());
        account.setEmail(request.getEmail());
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setPhone(request.getPhoneNumber());
        account.setGender(request.getGender());
        account.setDateOfBirth(request.getDateOfBirth());
        account.setRole(customerRole);
        account.setUpdatedDate(LocalDateTime.now());
        account.setVerified(false);

        Account savedAccount = accountRepository.save(account);

        // Tạo đơn đăng ký
        ArtisanApplication application = createApplication(savedAccount, request.getArtisanName(), 
            request.getBio(), request.getExperienceYear(), request.getPortfolioUrl(), request.getSpecialization());

        return mapToResponse(application, "Đăng ký thành công! Đơn đăng ký đang chờ admin xét duyệt.");
    }

    @Override
    @Transactional
    public ArtisanApplicationResponse submitApplication(ArtisanApplicationRequest request, UUID accountId) {
        // Kiểm tra đã là artisan chưa
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("Account not found"));
        if ("ARTISAN".equals(account.getRole().getName())) {
            throw new RuntimeException("Bạn đã là nghệ nhân rồi");
        }

        // Kiểm tra đơn pending
        if (applicationRepository.existsByAccountAndStatus(account, ApplicationStatus.PENDING)) {
            throw new RuntimeException("Bạn đã có đơn đăng ký đang chờ xét duyệt");
        }

        ArtisanApplication application = createApplication(account, request.getArtisanName(),
            request.getBio(), request.getExperienceYear(), request.getPortfolioUrl(), request.getSpecialization());

        return mapToResponse(application, "Đơn đăng ký đã được gửi thành công. Vui lòng chờ admin xét duyệt.");
    }

    @Override
    @Transactional
    public ArtisanApplicationResponse reviewApplication(UUID applicationId, ReviewApplicationRequest request, Account admin) {
        ArtisanApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đăng ký"));

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new RuntimeException("Đơn đăng ký này đã được xét duyệt");
        }

        application.setReviewedBy(admin);
        application.setReviewedDate(LocalDateTime.now());

        if (request.getApproved()) {
            // Phê duyệt
            application.setStatus(ApplicationStatus.APPROVED);
            
            Role artisanRole = roleRepository.findByName("ARTISAN")
                    .orElseThrow(() -> new RuntimeException("Role ARTISAN không tồn tại"));
            
            application.getAccount().setRole(artisanRole);
            application.getAccount().setVerified(true);
            application.getAccount().setUpdatedDate(LocalDateTime.now());

            // Tạo artisan profile
            Artisan artisan = new Artisan();
            artisan.setAccount(application.getAccount());
            artisan.setArtisanName(application.getArtisanName());
            artisan.setBio(application.getBio());
            artisan.setExperience_year(application.getExperienceYear());
            artisanRepository.save(artisan);

            applicationRepository.save(application);
            return mapToResponse(application, "Đơn đăng ký đã được phê duyệt thành công");
        } else {
            // Từ chối
            if (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty()) {
                throw new RuntimeException("Vui lòng nhập lý do từ chối");
            }
            application.setStatus(ApplicationStatus.REJECTED);
            application.setRejectionReason(request.getRejectionReason());
            
            applicationRepository.save(application);
            return mapToResponse(application, "Đơn đăng ký đã bị từ chối");
        }
    }

    @Override
    public List<ArtisanApplicationResponse> getPendingApplications() {
        return applicationRepository.findByStatus(ApplicationStatus.PENDING)
                .stream()
                .map(app -> mapToResponse(app, null))
                .collect(Collectors.toList());
    }

    @Override
    public List<ArtisanApplicationResponse> getMyApplications(UUID accountId) {
        return applicationRepository.findByAccount_AccountIdOrderBySubmittedDateDesc(accountId)
                .stream()
                .map(app -> mapToResponse(app, null))
                .collect(Collectors.toList());
    }

    @Override
    public ArtisanApplicationResponse getApplicationById(UUID applicationId) {
        ArtisanApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đăng ký"));
        return mapToResponse(application, null);
    }

    private ArtisanApplication createApplication(Account account, String artisanName, String bio, 
                                                 int experienceYear, String portfolioUrl, String specialization) {
        ArtisanApplication application = new ArtisanApplication();
        application.setAccount(account);
        application.setArtisanName(artisanName);
        application.setBio(bio);
        application.setExperienceYear(experienceYear);
        application.setPortfolioUrl(portfolioUrl);
        application.setSpecialization(specialization);
        application.setStatus(ApplicationStatus.PENDING);
        application.setSubmittedDate(LocalDateTime.now());
        return applicationRepository.save(application);
    }

    private ArtisanApplicationResponse mapToResponse(ArtisanApplication app, String message) {
        return ArtisanApplicationResponse.builder()
                .applicationId(app.getApplicationId())
                .accountId(app.getAccount().getAccountId())
                .accountEmail(app.getAccount().getEmail())
                .accountFullName(app.getAccount().getFullName())
                .artisanName(app.getArtisanName())
                .bio(app.getBio())
                .experienceYear(app.getExperienceYear())
                .portfolioUrl(app.getPortfolioUrl())
                .specialization(app.getSpecialization())
                .status(app.getStatus())
                .rejectionReason(app.getRejectionReason())
                .submittedDate(app.getSubmittedDate())
                .reviewedDate(app.getReviewedDate())
                .reviewedByName(app.getReviewedBy() != null ? app.getReviewedBy().getFullName() : null)
                .build();
    }
}
