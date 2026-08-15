package com.ers.security.ldap;

import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

import java.util.Collection;

/** Pulls cn (full name) and mail off the authenticated directory entry so /me can show a real
 * name/email for LDAP users without needing a local DB row to read them back from. */
public class DirectoryUserDetailsMapper implements UserDetailsContextMapper {

    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
                                           Collection<? extends GrantedAuthority> authorities) {
        String fullName = ctx.getStringAttribute("cn");
        String email = ctx.getStringAttribute("mail");
        return new ErsLdapUserDetails(username, fullName != null ? fullName : username, email, authorities);
    }

    @Override
    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
        throw new UnsupportedOperationException("ERS does not write back to the directory");
    }
}
