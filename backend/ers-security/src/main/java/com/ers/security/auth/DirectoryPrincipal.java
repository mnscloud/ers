package com.ers.security.auth;

/**
 * Implemented by authentication principals sourced from an external directory (LDAP/AD) that have
 * no corresponding local {@code User} row - lets AuthService read profile attributes without a
 * compile-time dependency on any specific directory integration.
 */
public interface DirectoryPrincipal {

    String getFullName();

    String getEmail();
}
