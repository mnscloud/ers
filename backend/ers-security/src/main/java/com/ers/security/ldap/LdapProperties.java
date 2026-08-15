package com.ers.security.ldap;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LDAP/Active Directory config. The embedded=true defaults stand up a throwaway UnboundID test
 * server seeded from demo-users.ldif, so this works locally with zero external setup. Pointing at
 * a real AD in production is a config-only change: embedded=false, urls/base/managerDn/managerPassword
 * set to the real directory, userDnPattern or a search-based lookup matching your directory layout,
 * and roleMappings renamed to your actual AD group names - no code changes.
 */
@Component
@ConfigurationProperties(prefix = "ers.security.ldap")
public class LdapProperties {

    private boolean enabled = true;
    private boolean embedded = true;
    private String embeddedLdif = "classpath:ldap/demo-users.ldif";
    private String embeddedBaseDn = "dc=ers,dc=local";
    private int embeddedPort = 11389;

    private String urls;
    private String base = "dc=ers,dc=local";
    private String groupSearchBase = "ou=groups";
    private String userDnPattern = "uid={0},ou=people";
    private String managerDn;
    private String managerPassword;

    /** Try LDAP before the local DB (local stays available as a break-glass fallback). */
    private boolean tryFirst = true;

    private Map<String, String> roleMappings = new LinkedHashMap<>(Map.of(
            "ERS-Admins", "ADMIN",
            "ERS-Makers", "RECON_MAKER",
            "ERS-Checkers", "RECON_CHECKER",
            "ERS-Compliance", "COMPLIANCE",
            "ERS-Viewers", "VIEWER"
    ));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public void setEmbedded(boolean embedded) {
        this.embedded = embedded;
    }

    public String getEmbeddedLdif() {
        return embeddedLdif;
    }

    public void setEmbeddedLdif(String embeddedLdif) {
        this.embeddedLdif = embeddedLdif;
    }

    public String getEmbeddedBaseDn() {
        return embeddedBaseDn;
    }

    public void setEmbeddedBaseDn(String embeddedBaseDn) {
        this.embeddedBaseDn = embeddedBaseDn;
    }

    public int getEmbeddedPort() {
        return embeddedPort;
    }

    public void setEmbeddedPort(int embeddedPort) {
        this.embeddedPort = embeddedPort;
    }

    public String getUrls() {
        return urls;
    }

    public void setUrls(String urls) {
        this.urls = urls;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getGroupSearchBase() {
        return groupSearchBase;
    }

    public void setGroupSearchBase(String groupSearchBase) {
        this.groupSearchBase = groupSearchBase;
    }

    public String getUserDnPattern() {
        return userDnPattern;
    }

    public void setUserDnPattern(String userDnPattern) {
        this.userDnPattern = userDnPattern;
    }

    public String getManagerDn() {
        return managerDn;
    }

    public void setManagerDn(String managerDn) {
        this.managerDn = managerDn;
    }

    public String getManagerPassword() {
        return managerPassword;
    }

    public void setManagerPassword(String managerPassword) {
        this.managerPassword = managerPassword;
    }

    public boolean isTryFirst() {
        return tryFirst;
    }

    public void setTryFirst(boolean tryFirst) {
        this.tryFirst = tryFirst;
    }

    public Map<String, String> getRoleMappings() {
        return roleMappings;
    }

    public void setRoleMappings(Map<String, String> roleMappings) {
        this.roleMappings = roleMappings;
    }
}
