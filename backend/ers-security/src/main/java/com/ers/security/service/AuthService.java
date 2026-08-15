package com.ers.security.service;

import com.ers.common.enums.AuditAction;
import com.ers.common.event.AuditLogEvent;
import com.ers.security.auth.DirectoryPrincipal;
import com.ers.security.domain.Permission;
import com.ers.security.domain.Role;
import com.ers.security.domain.User;
import com.ers.security.dto.CurrentUserResponse;
import com.ers.security.dto.TokenResponse;
import com.ers.security.jwt.JwtTokenProvider;
import com.ers.security.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final String ROLE_PREFIX = "ROLE_";

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final ApplicationEventPublisher eventPublisher;

    public AuthService(AuthenticationManager authenticationManager,
                        UserRepository userRepository,
                        JwtTokenProvider tokenProvider,
                        ApplicationEventPublisher eventPublisher) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public TokenResponse login(String username, String password) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
        } catch (AuthenticationException ex) {
            recordFailedAttempt(username);
            throw ex;
        }

        Optional<User> localUser = userRepository.findByUsernameIgnoreCase(username);
        String fullName;
        String email;
        List<String> roles;
        List<String> permissions;
        String auditEntityId;

        if (localUser.isPresent()) {
            User user = localUser.get();
            user.setFailedLoginAttempts(0);
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);

            fullName = user.getFullName();
            email = user.getEmail();
            roles = user.getRoles().stream().map(Role::getName).toList();
            permissions = rolesToPermissions(user.getRoles());
            auditEntityId = user.getId().toString();
        } else {
            // No local row: this identity was authenticated entirely via an external provider (LDAP/AD).
            Object principal = authentication.getPrincipal();
            fullName = principal instanceof DirectoryPrincipal directoryPrincipal ? directoryPrincipal.getFullName() : username;
            email = principal instanceof DirectoryPrincipal directoryPrincipal ? directoryPrincipal.getEmail() : null;
            roles = rolesFromAuthorities(authentication.getAuthorities());
            permissions = permissionsFromAuthorities(authentication.getAuthorities());
            auditEntityId = username;
        }

        String accessToken = tokenProvider.generateAccessToken(username, fullName, email, roles, permissions);
        String refreshToken = tokenProvider.generateRefreshToken(username, fullName, email, roles, permissions);

        eventPublisher.publishEvent(AuditLogEvent.of(username, AuditAction.LOGIN, "User", auditEntityId, "User logged in"));

        return new TokenResponse(accessToken, refreshToken, "Bearer",
                tokenProvider.getAccessTokenExpirationSeconds(), username, roles);
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(String refreshToken) {
        if (!tokenProvider.isValid(refreshToken) || !tokenProvider.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }
        Claims claims = tokenProvider.parseClaims(refreshToken);
        String username = claims.getSubject();

        String fullName;
        String email;
        List<String> roles;
        List<String> permissions;

        Optional<User> localUser = userRepository.findByUsernameIgnoreCase(username);
        if (localUser.isPresent()) {
            // Local users get their roles/permissions re-derived from the DB on every refresh, so a
            // permission change takes effect within one access-token lifetime rather than requiring
            // a fresh login. LDAP-only identities fall back to whatever the refresh token already
            // carries, since there's no password here to re-bind against the directory with.
            User user = localUser.get();
            fullName = user.getFullName();
            email = user.getEmail();
            roles = user.getRoles().stream().map(Role::getName).toList();
            permissions = rolesToPermissions(user.getRoles());
        } else {
            fullName = tokenProvider.getFullName(claims);
            email = tokenProvider.getEmail(claims);
            roles = tokenProvider.getRoles(claims);
            permissions = tokenProvider.getPermissions(claims);
        }

        String accessToken = tokenProvider.generateAccessToken(username, fullName, email, roles, permissions);
        String newRefreshToken = tokenProvider.generateRefreshToken(username, fullName, email, roles, permissions);

        return new TokenResponse(accessToken, newRefreshToken, "Bearer",
                tokenProvider.getAccessTokenExpirationSeconds(), username, roles);
    }

    /** Builds the /me response straight from an already-validated JWT - no DB round-trip, works
     * identically regardless of whether the session came from local or LDAP auth. */
    public CurrentUserResponse currentUserFromClaims(Claims claims) {
        return new CurrentUserResponse(
                null,
                claims.getSubject(),
                tokenProvider.getEmail(claims),
                tokenProvider.getFullName(claims),
                tokenProvider.getRoles(claims),
                tokenProvider.getPermissions(claims)
        );
    }

    private void recordFailedAttempt(String username) {
        Optional<User> localUser = userRepository.findByUsernameIgnoreCase(username);
        if (localUser.isPresent()) {
            User user = localUser.get();
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.setAccountLocked(true);
            }
            userRepository.save(user);
            eventPublisher.publishEvent(AuditLogEvent.of(username, AuditAction.LOGIN_FAILED, "User", user.getId().toString(),
                    "Failed login attempt (" + user.getFailedLoginAttempts() + "/" + MAX_FAILED_ATTEMPTS + ")"));
        } else {
            eventPublisher.publishEvent(AuditLogEvent.of(username, AuditAction.LOGIN_FAILED, "User", username,
                    "Failed login attempt (no local account - possibly an unknown or LDAP-only identity)"));
        }
    }

    private List<String> rolesToPermissions(java.util.Set<Role> roles) {
        return roles.stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getName)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> rolesFromAuthorities(java.util.Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith(ROLE_PREFIX))
                .map(a -> a.substring(ROLE_PREFIX.length()))
                .toList();
    }

    private List<String> permissionsFromAuthorities(java.util.Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith(ROLE_PREFIX))
                .toList();
    }
}
