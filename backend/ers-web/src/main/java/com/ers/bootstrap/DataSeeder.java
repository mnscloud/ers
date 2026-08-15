package com.ers.bootstrap;

import com.ers.security.domain.Permission;
import com.ers.security.domain.Role;
import com.ers.security.domain.User;
import com.ers.security.repository.PermissionRepository;
import com.ers.security.repository.RoleRepository;
import com.ers.security.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bootstraps the minimum RBAC data and a default admin account so a freshly migrated database is
 * immediately usable. Seeds are idempotent (checked by name/username) so this is safe to run on
 * every startup, including against an already-seeded database. Runs before {@link SampleDataSeeder},
 * which depends on the ADMIN role existing.
 */
@Component
@Order(1)
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final Map<String, List<String>> ROLE_PERMISSIONS = Map.of(
            "ADMIN", List.of("INGESTION_WRITE", "MATCHING_CONFIGURE", "MATCHING_RUN", "EXCEPTION_TRIAGE",
                    "ADJUSTMENT_CREATE", "ADJUSTMENT_APPROVE", "ADMIN_USERS", "MASTERDATA_WRITE", "MASTERDATA_APPROVE"),
            "RECON_MAKER", List.of("INGESTION_WRITE", "MATCHING_CONFIGURE", "MATCHING_RUN", "EXCEPTION_TRIAGE",
                    "ADJUSTMENT_CREATE", "MASTERDATA_WRITE"),
            "RECON_CHECKER", List.of("EXCEPTION_TRIAGE", "ADJUSTMENT_APPROVE", "MASTERDATA_APPROVE"),
            "COMPLIANCE", List.of("ADJUSTMENT_APPROVE", "ADMIN_USERS", "MASTERDATA_APPROVE"),
            "VIEWER", List.of()
    );

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ers.security.default-admin-username:admin}")
    private String defaultAdminUsername;

    @Value("${ers.security.default-admin-password:ChangeMe123!}")
    private String defaultAdminPassword;

    public DataSeeder(PermissionRepository permissionRepository, RoleRepository roleRepository,
                       UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, Permission> permissions = ROLE_PERMISSIONS.values().stream()
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toMap(name -> name, this::ensurePermission));

        ROLE_PERMISSIONS.forEach((roleName, permissionNames) -> {
            Set<Permission> perms = permissionNames.stream().map(permissions::get).collect(Collectors.toSet());
            ensureRole(roleName, perms);
        });

        ensureDefaultAdmin();
    }

    private Permission ensurePermission(String name) {
        return permissionRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> permissionRepository.save(new Permission(name, name.replace('_', ' ') + " permission")));
    }

    private void ensureRole(String name, Set<Permission> permissions) {
        Role role = roleRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> new Role(name, name + " role"));
        role.setPermissions(permissions);
        roleRepository.save(role);
    }

    private void ensureDefaultAdmin() {
        if (userRepository.existsByUsernameIgnoreCase(defaultAdminUsername)) {
            return;
        }
        Role adminRole = roleRepository.findByNameIgnoreCase("ADMIN")
                .orElseThrow(() -> new IllegalStateException("ADMIN role must be seeded before the default admin user"));

        User admin = new User();
        admin.setUsername(defaultAdminUsername);
        admin.setEmail(defaultAdminUsername + "@ers.local");
        admin.setFullName("System Administrator");
        admin.setPasswordHash(passwordEncoder.encode(defaultAdminPassword));
        admin.setRoles(Set.of(adminRole));
        userRepository.save(admin);

        log.warn("Seeded default admin user '{}' with the configured default password - change it immediately in a real environment.",
                defaultAdminUsername);
    }
}
