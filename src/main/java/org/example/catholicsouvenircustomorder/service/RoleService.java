package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.dto.request.RoleRequest;
import org.example.catholicsouvenircustomorder.dto.response.RoleResponse;
import org.example.catholicsouvenircustomorder.model.Role;

import java.util.List;

public interface RoleService {
     RoleResponse addRole(RoleRequest roleRequest);
     Role updateRole(int roleId, RoleRequest roleRequest);
     void deleteRole(int roleId);
     List<RoleResponse> getAllRole();
     RoleResponse convertToResponseDTO(Role role);
}
