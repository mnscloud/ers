package com.ers.compliance.audit;

import com.ers.common.event.AuditLogEvent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class AuditAspect {

    private final ApplicationEventPublisher eventPublisher;

    public AuditAspect(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        Object result = joinPoint.proceed();

        String entityId = extractId(result);
        String actor = currentUser();

        eventPublisher.publishEvent(AuditLogEvent.of(
                actor, audited.action(), audited.entityType(), entityId,
                audited.entityType() + " " + audited.action().name().toLowerCase() + " by " + actor));

        return result;
    }

    private String extractId(Object result) {
        if (result == null) {
            return "unknown";
        }
        try {
            Method getId = result.getClass().getMethod("getId");
            Object id = getId.invoke(result);
            return id != null ? id.toString() : "unknown";
        } catch (ReflectiveOperationException ex) {
            return "unknown";
        }
    }

    private String currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
