package org.example.catholicsouvenircustomorder.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.BaseResponse;
import org.example.catholicsouvenircustomorder.dto.request.RoleRequest;
import org.example.catholicsouvenircustomorder.dto.response.RoleResponse;
import org.example.catholicsouvenircustomorder.model.Role;
import org.example.catholicsouvenircustomorder.service.RoleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse> getAllRoles() {
        try {
            List<RoleResponse> roles = roleService.getAllRole();
            return ResponseEntity.ok(BaseResponse.success("Lấy danh sách role thành công", roles));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(BaseResponse.error(500, "Lỗi khi lấy danh sách role: " + e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse> addRole(@Valid @RequestBody RoleRequest roleRequest) {
        try {
            RoleResponse response = roleService.addRole(roleRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(BaseResponse.success("Thêm role thành công", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.error(400, e.getMessage()));
        }
    }

    @PutMapping("/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse> updateRole(
            @PathVariable int roleId,
            @Valid @RequestBody RoleRequest roleRequest) {
        try {
            Role updatedRole = roleService.updateRole(roleId, roleRequest);
            RoleResponse response = roleService.convertToResponseDTO(updatedRole);
            return ResponseEntity.ok(BaseResponse.success("Cập nhật role thành công", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.error(400, e.getMessage()));
        }
    }

    @DeleteMapping("/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse> deleteRole(@PathVariable int roleId) {
        try {
            roleService.deleteRole(roleId);
            return ResponseEntity.ok(BaseResponse.success("Xóa role thành công"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(BaseResponse.error(400, e.getMessage()));
        }
    }
}
