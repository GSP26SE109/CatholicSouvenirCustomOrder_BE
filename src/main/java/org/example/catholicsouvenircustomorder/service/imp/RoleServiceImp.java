package org.example.catholicsouvenircustomorder.service.imp;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.dto.request.RoleRequest;
import org.example.catholicsouvenircustomorder.dto.response.RoleResponse;
import org.example.catholicsouvenircustomorder.exception.InsertException;
import org.example.catholicsouvenircustomorder.model.Role;
import org.example.catholicsouvenircustomorder.repository.RoleRepository;
import org.example.catholicsouvenircustomorder.service.RoleService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImp implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    public RoleResponse addRole(RoleRequest roleRequest) {
        Optional<Role> existedRole = roleRepository.findByName(roleRequest.getName());
        if (existedRole.isPresent()) {
            throw new InsertException("Role đã tồn tại");
        }

        try {
            Role role = new Role();
            role.setName(roleRequest.getName());
            role = roleRepository.save(role);

            return convertToResponseDTO(role);
        } catch (Exception e) {
            throw new InsertException("Lỗi khi thêm role: " + e.getMessage());
        }
    }

    @Override
    public Role updateRole(int roleId, RoleRequest roleRequest) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role với ID " + roleId + " không tồn tại"));

        Optional<Role> existingRole = roleRepository.findByName(roleRequest.getName());
        if (existingRole.isPresent() && existingRole.get().getRoleId() != roleId) {
            throw new RuntimeException("Tên role đã tồn tại");
        }
        
        role.setName(roleRequest.getName());
        return roleRepository.save(role);
    }

    @Override
    public void deleteRole(int roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role với ID " + roleId + " không tồn tại"));

        if (!role.getAccounts().isEmpty()) {
            throw new RuntimeException("Không thể xóa role đang được sử dụng bởi " + role.getAccounts().size() + " tài khoản");
        }
        
        roleRepository.delete(role);
    }

    @Override
    public List<RoleResponse> getAllRole() {
        List<Role> roles = roleRepository.findAll();
        return roles.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public RoleResponse convertToResponseDTO(Role role) {
        return RoleResponse.builder()
                .id(role.getRoleId())
                .name(role.getName())
                .build();
    }
}
