package com.ers.security.ldap;

import com.ers.security.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.server.UnboundIdContainer;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

/**
 * Conditional on ers.security.ldap.enabled (default true). To point this at a real Active
 * Directory instead of the embedded test server: set embedded=false, urls/base to the real AD,
 * managerDn/managerPassword to a service bind account, userDnPattern (or switch to a search-based
 * authenticator) to match the real directory layout, and roleMappings to the real AD group names.
 * No code changes needed.
 */
@Configuration
@ConditionalOnProperty(prefix = "ers.security.ldap", name = "enabled", havingValue = "true")
public class LdapAuthConfig {

    private final LdapProperties properties;

    public LdapAuthConfig(LdapProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnProperty(prefix = "ers.security.ldap", name = "embedded", havingValue = "true", matchIfMissing = true)
    public UnboundIdContainer embeddedLdapServer() {
        UnboundIdContainer container = new UnboundIdContainer(properties.getEmbeddedBaseDn(), properties.getEmbeddedLdif());
        container.setPort(properties.getEmbeddedPort());
        return container;
    }

    @Bean
    public LdapContextSource ersLdapContextSource() {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(properties.isEmbedded()
                ? "ldap://localhost:" + properties.getEmbeddedPort()
                : properties.getUrls());
        contextSource.setBase(properties.getBase());
        if (properties.getManagerDn() != null && !properties.getManagerDn().isBlank()) {
            contextSource.setUserDn(properties.getManagerDn());
            contextSource.setPassword(properties.getManagerPassword());
        }
        contextSource.afterPropertiesSet();
        return contextSource;
    }

    @Bean
    public LdapAuthoritiesPopulator ersLdapAuthoritiesPopulator(@Qualifier("ersLdapContextSource") LdapContextSource contextSource,
                                                                 RoleRepository roleRepository) {
        return new RoleMappingAuthoritiesPopulator(contextSource, properties.getGroupSearchBase(),
                properties.getRoleMappings(), roleRepository);
    }

    @Bean
    public LdapAuthenticationProvider ldapAuthenticationProvider(@Qualifier("ersLdapContextSource") LdapContextSource contextSource,
                                                                  LdapAuthoritiesPopulator authoritiesPopulator) {
        BindAuthenticator authenticator = new BindAuthenticator(contextSource);
        authenticator.setUserDnPatterns(new String[]{properties.getUserDnPattern()});
        LdapAuthenticationProvider provider = new LdapAuthenticationProvider(authenticator, authoritiesPopulator);
        provider.setUserDetailsContextMapper(new DirectoryUserDetailsMapper());
        return provider;
    }
}
