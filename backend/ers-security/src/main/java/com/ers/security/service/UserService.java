package com.ers.security.service;

import com.ers.common.enums.AuditAction;
import com.ers.common.event.AuditLogEvent;
import com.ers.common.exception.BusinessException;
import com.ers.common.exception.ResourceNotFoundException;
import com.ers.security.domain.Role;
import com.ers.security.domain.User;
import com.ers.security.dto.CreateUserRequest;
import com.ers.security.dto.UpdateUserRolesRequest;
import com.ers.security.dto.UserResponse;
import com.ers.security.repository.RoleRepository;
import com.ers.security.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder,
                        ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new BusinessException("USERNAME_TAKEN", "Username already exists: " + request.username());
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException("EMAIL_TAKEN", "Email already registered: " + request.email());
        }
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setFullName(request.fullName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(resolveRoles(request.roles()));
        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public UserResponse updateRoles(UUID userId, UpdateUserRolesRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        user.setRoles(resolveRoles(request.roles()));
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse setEnabled(UUID userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        user.setEnabled(enabled);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse unlock(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void changeOwnPassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new BusinessException("LDAP_MANAGED_ACCOUNT",
                        "Your password is managed by your LDAP/AD directory and cannot be changed here."));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BusinessException("INVALID_CURRENT_PASSWORD", "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        eventPublisher.publishEvent(AuditLogEvent.of(username, AuditAction.UPDATE, "User",
                user.getId().toString(), "Password changed"));
    }

    private Set<Role> resolveRoles(Set<String> roleNames) {
        return roleNames.stream()
                .map(name -> roleRepository.findByNameIgnoreCase(name)
                        .orElseThrow(() -> new BusinessException("UNKNOWN_ROLE", "Unknown role: " + name)))
                .collect(Collectors.toSet());
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(), user.getUsername(), user.getEmail(), user.getFullName(),
                user.isEnabled(), user.isAccountLocked(), user.getLastLoginAt(),
                user.getRoles().stream().map(Role::getName).toList()
        );
    }
}
