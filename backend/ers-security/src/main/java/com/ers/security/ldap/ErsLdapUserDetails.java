package com.ers.security.ldap;

import com.ers.security.auth.DirectoryPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/** Principal for LDAP/AD-authenticated sessions - carries directory attributes (full name, email)
 * that have nowhere else to live since these identities have no local User row. */
public class ErsLdapUserDetails implements UserDetails, DirectoryPrincipal {

    private final String username;
    private final String fullName;
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;

    public ErsLdapUserDetails(String username, String fullName, String email,
                               Collection<? extends GrantedAuthority> authorities) {
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.authorities = authorities;
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
