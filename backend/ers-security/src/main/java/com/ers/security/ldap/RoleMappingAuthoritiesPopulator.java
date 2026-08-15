package com.ers.security.ldap;

import com.ers.security.repository.RoleRepository;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * LDAP only decides *which app roles* apply (via the configured group-name -> role-name mapping);
 * the local Role/Permission tables (already seeded by DataSeeder) remain the single source of truth
 * for what each role can actually do. This keeps RBAC consistent regardless of auth source.
 */
public class RoleMappingAuthoritiesPopulator implements LdapAuthoritiesPopulator {

    private final DefaultLdapAuthoritiesPopulator delegate;
    private final Map<String, String> roleMappings;
    private final RoleRepository roleRepository;

    public RoleMappingAuthoritiesPopulator(ContextSource contextSource, String groupSearchBase,
                                            Map<String, String> roleMappings, RoleRepository roleRepository) {
        this.delegate = new DefaultLdapAuthoritiesPopulator(contextSource, groupSearchBase);
        this.delegate.setGroupRoleAttribute("cn");
        this.delegate.setGroupSearchFilter("(member={0})");
        this.delegate.setRolePrefix("");
        this.delegate.setConvertToUpperCase(false);
        this.delegate.setSearchSubtree(true);
        this.roleMappings = roleMappings;
        this.roleRepository = roleRepository;
    }

    @Override
    public Collection<? extends GrantedAuthority> getGrantedAuthorities(DirContextOperations userData, String username) {
        Set<String> matchedRoleNames = delegate.getGrantedAuthorities(userData, username).stream()
                .map(GrantedAuthority::getAuthority)
                .map(roleMappings::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String roleName : matchedRoleNames) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
            roleRepository.findByNameIgnoreCase(roleName).ifPresent(role ->
                    role.getPermissions().forEach(p -> authorities.add(new SimpleGrantedAuthority(p.getName()))));
        }
        return authorities;
    }
}
