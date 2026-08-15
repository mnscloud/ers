package com.ers.compliance.service;

import com.ers.common.enums.AuditAction;
import com.ers.common.event.AuditLogEvent;
import com.ers.common.exception.BusinessException;
import com.ers.common.exception.ResourceNotFoundException;
import com.ers.compliance.domain.AccountingPeriod;
import com.ers.compliance.domain.PeriodStatus;
import com.ers.compliance.repository.AccountingPeriodRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class PeriodLockService {

    private final AccountingPeriodRepository periodRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PeriodLockService(AccountingPeriodRepository periodRepository, ApplicationEventPublisher eventPublisher) {
        this.periodRepository = periodRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<AccountingPeriod> list() {
        return periodRepository.findAll();
    }

    /**
     * Used by write-side services in other modules (e.g. adjustment posting) to block writes
     * against a closed period. Creates the period as OPEN on first reference.
     */
    @Transactional
    public void assertOpen(String periodCode) {
        AccountingPeriod period = getOrCreate(periodCode);
        if (period.getStatus() == PeriodStatus.LOCKED) {
            throw new BusinessException("PERIOD_LOCKED", "Accounting period " + periodCode + " is locked for changes");
        }
    }

    @Transactional
    public AccountingPeriod getOrCreate(String periodCode) {
        return periodRepository.findByPeriodCode(periodCode).orElseGet(() -> {
            YearMonth ym = YearMonth.parse(periodCode);
            AccountingPeriod period = new AccountingPeriod();
            period.setPeriodCode(periodCode);
            period.setStartDate(ym.atDay(1));
            period.setEndDate(ym.atEndOfMonth());
            period.setStatus(PeriodStatus.OPEN);
            return periodRepository.save(period);
        });
    }

    @Transactional
    public AccountingPeriod lock(String periodCode) {
        AccountingPeriod period = getOrCreate(periodCode);
        period.setStatus(PeriodStatus.LOCKED);
        period.setLockedBy(currentUser());
        period.setLockedAt(Instant.now());
        AccountingPeriod saved = periodRepository.save(period);
        eventPublisher.publishEvent(AuditLogEvent.of(currentUser(), AuditAction.LOCK_PERIOD,
                "AccountingPeriod", periodCode, "Period locked"));
        return saved;
    }

    @Transactional
    public AccountingPeriod unlock(String periodCode) {
        AccountingPeriod period = periodRepository.findByPeriodCode(periodCode)
                .orElseThrow(() -> ResourceNotFoundException.of("AccountingPeriod", periodCode));
        period.setStatus(PeriodStatus.OPEN);
        period.setLockedBy(null);
        period.setLockedAt(null);
        AccountingPeriod saved = periodRepository.save(period);
        eventPublisher.publishEvent(AuditLogEvent.of(currentUser(), AuditAction.UNLOCK_PERIOD,
                "AccountingPeriod", periodCode, "Period unlocked"));
        return saved;
    }

    public static String periodCodeFor(LocalDate date) {
        return YearMonth.from(date).toString();
    }

    private String currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
