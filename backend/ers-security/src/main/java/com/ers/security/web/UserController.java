package com.ers.security.web;

import com.ers.common.web.ApiResponse;
import com.ers.common.web.PageResponse;
import com.ers.security.dto.CreateUserRequest;
import com.ers.security.dto.UpdateUserRolesRequest;
import com.ers.security.dto.UserResponse;
import com.ers.security.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasAuthority('ADMIN_USERS')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> list(Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(userService.list(pageable)));
    }

    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok(userService.create(request));
    }

    @PutMapping("/{id}/roles")
    public ApiResponse<UserResponse> updateRoles(@PathVariable UUID id, @Valid @RequestBody UpdateUserRolesRequest request) {
        return ApiResponse.ok(userService.updateRoles(id, request));
    }

    @PatchMapping("/{id}/enabled")
    public ApiResponse<UserResponse> setEnabled(@PathVariable UUID id, @RequestParam boolean enabled) {
        return ApiResponse.ok(userService.setEnabled(id, enabled));
    }

    @PatchMapping("/{id}/unlock")
    public ApiResponse<UserResponse> unlock(@PathVariable UUID id) {
        return ApiResponse.ok(userService.unlock(id));
    }
}
