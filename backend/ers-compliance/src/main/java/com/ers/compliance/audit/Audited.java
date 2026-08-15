package com.ers.compliance.audit;

import com.ers.common.enums.AuditAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method whose successful invocation should append an immutable audit trail row.
 * The annotated method's return value is inspected via reflection for a getId() to use as entityId.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Audited {

    AuditAction action();

    String entityType();
}
