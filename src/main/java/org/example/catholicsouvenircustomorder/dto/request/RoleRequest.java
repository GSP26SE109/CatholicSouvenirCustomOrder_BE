package org.example.catholicsouvenircustomorder.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleRequest {
    @NotBlank(message = "Tên role không được để trống")
    private String name;
}
