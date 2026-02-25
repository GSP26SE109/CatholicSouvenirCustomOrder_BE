package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterArtisanRequest {
    // Thông tin tài khoản
    @NotBlank(message = "Họ không được để trống")
    private String firstName;

    @NotBlank(message = "Tên không được để trống")
    private String lastName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    private String confirmPassword;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String phoneNumber;

    private String gender;
    private String dateOfBirth;

    // Thông tin artisan
    @NotBlank(message = "Tên nghệ nhân không được để trống")
    private String artisanName;

    @NotBlank(message = "Giới thiệu không được để trống")
    @Size(max = 1000, message = "Giới thiệu không được vượt quá 1000 ký tự")
    private String bio;

    @NotNull(message = "Số năm kinh nghiệm không được để trống")
    @Min(value = 0, message = "Số năm kinh nghiệm phải lớn hơn hoặc bằng 0")
    private Integer experienceYear;

    private String portfolioUrl;

    @NotBlank(message = "Chuyên môn không được để trống")
    private String specialization;
}
