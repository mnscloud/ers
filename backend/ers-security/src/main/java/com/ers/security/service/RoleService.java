package com.ers.security.service;

import com.ers.security.domain.Permission;
import com.ers.security.dto.RoleResponse;
import com.ers.security.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> list() {
        return roleRepository.findAll().stream()
                .map(role -> new RoleResponse(
                        role.getId(), role.getName(), role.getDescription(),
                        role.getPermissions().stream().map(Permission::getName).toList()))
                .toList();
    }
}
