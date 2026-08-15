package com.ers.bootstrap;

import com.ers.adjustment.domain.DebitCredit;
import com.ers.adjustment.domain.JournalEntry;
import com.ers.adjustment.dto.CreateJournalEntryRequest;
import com.ers.adjustment.service.AdjustmentService;
import com.ers.common.enums.MatchType;
import com.ers.common.enums.ReconciliationType;
import com.ers.exception.domain.ReconciliationBreak;
import com.ers.exception.repository.ReconciliationBreakRepository;
import com.ers.exception.service.BreakTriageService;
import com.ers.ingestion.domain.BatchStatus;
import com.ers.ingestion.domain.DataSource;
import com.ers.ingestion.domain.DataSourceType;
import com.ers.ingestion.domain.FileFormat;
import com.ers.ingestion.domain.IngestionBatch;
import com.ers.ingestion.domain.RawTransactionRecord;
import com.ers.ingestion.dto.DataSourceRequest;
import com.ers.ingestion.repository.DataSourceRepository;
import com.ers.ingestion.repository.IngestionBatchRepository;
import com.ers.ingestion.repository.RawTransactionRecordRepository;
import com.ers.ingestion.service.DataSourceService;
import com.ers.masterdata.dto.MasterDataRequest;
import com.ers.masterdata.service.CounterpartyService;
import com.ers.masterdata.service.CurrencyService;
import com.ers.masterdata.service.GlAccountService;
import com.ers.masterdata.service.TransactionTypeService;
import com.ers.matching.domain.MatchRule;
import com.ers.matching.dto.MatchRuleRequest;
import com.ers.matching.service.MatchRuleService;
import com.ers.reconciliation.domain.Reconciliation;
import com.ers.reconciliation.domain.ReconciliationTemplate;
import com.ers.reconciliation.dto.CreateReconciliationRequest;
import com.ers.reconciliation.dto.ReconciliationTemplateRequest;
import com.ers.reconciliation.service.ReconciliationService;
import com.ers.reconciliation.service.ReconciliationTemplateService;
import com.ers.security.dto.CreateUserRequest;
import com.ers.security.repository.UserRepository;
import com.ers.security.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Populates a rich demo dataset by driving the real application services (not raw SQL), so it
 * exercises the exact same code paths as a real user would: uploading/ingesting, running the
 * matching engine, triggering reconciliations (which auto-generate exceptions via the normal event
 * flow), triaging breaks across every status, and posting adjustments through maker-checker.
 *
 * Runs after {@link DataSeeder} (RBAC bootstrap). Gated by ers.demo.enabled (default true) so it
 * can be switched off for a deployment that shouldn't carry demo data.
 */
@Component
@Order(2)
@ConditionalOnProperty(prefix = "ers.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SampleDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleDataSeeder.class);

    private final UserRepository userRepository;
    private final UserService userService;
    private final DataSourceRepository dataSourceRepository;
    private final DataSourceService dataSourceService;
    private final IngestionBatchRepository batchRepository;
    private final RawTransactionRecordRepository recordRepository;
    private final MatchRuleService matchRuleService;
    private final ReconciliationTemplateService templateService;
    private final ReconciliationService reconciliationService;
    private final ReconciliationBreakRepository breakRepository;
    private final BreakTriageService breakTriageService;
    private final AdjustmentService adjustmentService;
    private final TransactionTypeService transactionTypeService;
    private final GlAccountService glAccountService;
    private final CurrencyService currencyService;
    private final CounterpartyService counterpartyService;

    public SampleDataSeeder(UserRepository userRepository, UserService userService,
                             DataSourceRepository dataSourceRepository, DataSourceService dataSourceService,
                             IngestionBatchRepository batchRepository, RawTransactionRecordRepository recordRepository,
                             MatchRuleService matchRuleService, ReconciliationTemplateService templateService,
                             ReconciliationService reconciliationService, ReconciliationBreakRepository breakRepository,
                             BreakTriageService breakTriageService, AdjustmentService adjustmentService,
                             TransactionTypeService transactionTypeService, GlAccountService glAccountService,
                             CurrencyService currencyService, CounterpartyService counterpartyService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.dataSourceRepository = dataSourceRepository;
        this.dataSourceService = dataSourceService;
        this.batchRepository = batchRepository;
        this.recordRepository = recordRepository;
        this.matchRuleService = matchRuleService;
        this.templateService = templateService;
        this.reconciliationService = reconciliationService;
        this.breakRepository = breakRepository;
        this.breakTriageService = breakTriageService;
        this.adjustmentService = adjustmentService;
        this.transactionTypeService = transactionTypeService;
        this.glAccountService = glAccountService;
        this.currencyService = currencyService;
        this.counterpartyService = counterpartyService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (dataSourceRepository.count() > 0) {
            log.info("Sample data already present - skipping SampleDataSeeder.");
            return;
        }

        seedDemoUsers();

        DataSource internalCash = createDataSource("Internal Cash Ledger", "INTERNAL_CASH");
        DataSource bankStatement = createDataSource("Bank Statement Feed", "BANK_STATEMENT");
        DataSource glTrialBalance = createDataSource("GL Trial Balance", "GL_TRIAL_BALANCE");
        DataSource subledgerAp = createDataSource("Subledger AP", "SUBLEDGER_AP");
        DataSource subsidiaryA = createDataSource("Subsidiary A Ledger", "SUBSIDIARY_A");
        DataSource subsidiaryB = createDataSource("Subsidiary B Ledger", "SUBSIDIARY_B");

        MatchRule bankCashRule = matchRuleService.create(new MatchRuleRequest(
                "Bank Cash Match", "INTERNAL_CASH", "BANK_STATEMENT", MatchType.ONE_TO_ONE,
                new BigDecimal("0.01"), 2), "admin");
        matchRuleService.approve(bankCashRule.getId(), "checker1", "Approved for demo use.");
        MatchRule strictRule = matchRuleService.create(new MatchRuleRequest(
                "Strict Same-Day Match", "INTERNAL_CASH", "BANK_STATEMENT", MatchType.ONE_TO_ONE,
                BigDecimal.ZERO, 0), "admin");
        matchRuleService.approve(strictRule.getId(), "checker1", "Approved for demo use.");

        ReconciliationTemplate bankCashTemplate = templateService.create(new ReconciliationTemplateRequest(
                "Monthly Bank Recon", ReconciliationType.BANK_CASH, bankCashRule.getId(), "admin"), "admin");
        templateService.approve(bankCashTemplate.getId(), "checker1", "Approved for demo use.");
        ReconciliationTemplate glTemplate = templateService.create(new ReconciliationTemplateRequest(
                "Monthly GL Recon", ReconciliationType.GENERAL_LEDGER, null, "compliance1"), "admin");
        templateService.approve(glTemplate.getId(), "checker1", "Approved for demo use.");
        templateService.create(new ReconciliationTemplateRequest(
                "Intercompany Elimination", ReconciliationType.INTERCOMPANY, null, "compliance1"), "admin");
        // Left PENDING intentionally so the master-data approval queue has something to act on.

        seedMasterData();

        // Prior period: a fully closed-out reconciliation (all breaks resolved, adjustment posted) -
        // shows what a finished period looks like.
        String priorPeriod = YearMonth.now().minusMonths(1).toString();
        seedBankCashRecords(internalCash, bankStatement, priorPeriod, PRIOR_PERIOD_LINES, 1);
        Reconciliation priorRecon = reconciliationService.trigger(reconciliationService.create(
                new CreateReconciliationRequest(bankCashTemplate.getId(), priorPeriod)).getId());
        List<ReconciliationBreak> priorBreaks = breakRepository.findByReconciliationId(priorRecon.getId());
        for (ReconciliationBreak b : priorBreaks) {
            breakTriageService.assign(b.getId(), "maker1", "admin");
            breakTriageService.resolve(b.getId(), "Confirmed as a timing difference; no adjustment needed.", "maker1");
        }
        JournalEntry priorAdjustment = adjustmentService.create(new CreateJournalEntryRequest(
                priorBreaks.isEmpty() ? null : priorBreaks.get(0).getId(), "1000-CASH", DebitCredit.DEBIT,
                new BigDecimal("1500.00"), "USD", "Prior period write-up, timing difference cleared", priorPeriod), "admin");
        adjustmentService.approveAndPost(priorAdjustment.getId(), "checker1", "Reviewed and approved - prior period close.");

        // Current period: an in-progress reconciliation with breaks in every status, so the
        // Exceptions page shows real variety instead of everything sitting OPEN.
        String currentPeriod = YearMonth.now().toString();
        seedBankCashRecords(internalCash, bankStatement, currentPeriod, CURRENT_PERIOD_LINES, 4);
        Reconciliation currentRecon = reconciliationService.trigger(reconciliationService.create(
                new CreateReconciliationRequest(bankCashTemplate.getId(), currentPeriod)).getId());
        triageCurrentPeriodBreaks(currentRecon.getId());

        // A maker-created entry still awaiting a checker, and one the checker sent back - shows
        // both open ends of the maker-checker workflow, not just the fully-posted happy path.
        adjustmentService.create(new CreateJournalEntryRequest(
                null, "6100-FEES", DebitCredit.CREDIT, new BigDecimal("250.00"), "USD",
                "Reclass of misposted bank fee", currentPeriod), "maker1");
        JournalEntry toReject = adjustmentService.create(new CreateJournalEntryRequest(
                null, "2100-ACCRUAL", DebitCredit.DEBIT, new BigDecimal("980.00"), "USD",
                "Accrual reversal - pending review", currentPeriod), "maker1");
        adjustmentService.reject(toReject.getId(), "checker1", "Needs supporting documentation before this can be posted.");

        seedOtherSourceRecords(glTrialBalance, "GL-", "GL_TRIAL_BALANCE", currentPeriod);
        seedOtherSourceRecords(subledgerAp, "AP-", "SUBLEDGER_AP", currentPeriod);
        seedOtherSourceRecords(subsidiaryA, "ICA-", "SUBSIDIARY_A", currentPeriod);
        seedOtherSourceRecords(subsidiaryB, "ICB-", "SUBSIDIARY_B", currentPeriod);

        log.info("Seeded sample data: prior period {} matched/{} unmatched, current period {} matched/{} unmatched",
                priorRecon.getMatchedCount(), priorRecon.getUnmatchedCount(),
                currentRecon.getMatchedCount(), currentRecon.getUnmatchedCount());
    }

    private void seedDemoUsers() {
        seedUser("maker1", "maker1@ers.local", "Maria Maker", "Maker@12345", "RECON_MAKER");
        seedUser("checker1", "checker1@ers.local", "Chris Checker", "Checker@12345", "RECON_CHECKER");
        seedUser("compliance1", "compliance1@ers.local", "Carla Compliance", "Compliance@12345", "COMPLIANCE");
        seedUser("viewer1", "viewer1@ers.local", "Victor Viewer", "Viewer@12345", "VIEWER");
    }

    private void seedUser(String username, String email, String fullName, String password, String role) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            return;
        }
        userService.create(new CreateUserRequest(username, email, fullName, password, Set.of(role)));
    }

    private DataSource createDataSource(String name, String sourceSystem) {
        DataSource dataSource = dataSourceService.create(
                new DataSourceRequest(name, sourceSystem, DataSourceType.FILE, FileFormat.CSV, null), "admin");
        return dataSourceService.approve(dataSource.getId(), "checker1", "Approved for demo use.");
    }

    /** Reference data for the account codes/currencies already used elsewhere in this seeder, plus
     * one pending row per type so the master-data approval queue has something real to act on. */
    private void seedMasterData() {
        var payment = transactionTypeService.create(new MasterDataRequest("PAYMENT", "Payment", "Outbound vendor/supplier payment"), "maker1");
        transactionTypeService.approve(payment.getId(), "checker1", "Approved for demo use.");
        var receipt = transactionTypeService.create(new MasterDataRequest("RECEIPT", "Receipt", "Inbound customer receipt"), "maker1");
        transactionTypeService.approve(receipt.getId(), "checker1", "Approved for demo use.");
        var fee = transactionTypeService.create(new MasterDataRequest("FEE", "Bank Fee", "Bank or service charge"), "maker1");
        transactionTypeService.approve(fee.getId(), "checker1", "Approved for demo use.");
        transactionTypeService.create(new MasterDataRequest("FX", "FX Revaluation", "Foreign exchange revaluation entry"), "maker1");
        // FX left PENDING intentionally.

        var cash = glAccountService.create(new MasterDataRequest("1000-CASH", "Cash and Cash Equivalents", null), "maker1");
        glAccountService.approve(cash.getId(), "checker1", "Approved for demo use.");
        var fees = glAccountService.create(new MasterDataRequest("6100-FEES", "Bank Fee Expense", null), "maker1");
        glAccountService.approve(fees.getId(), "checker1", "Approved for demo use.");
        glAccountService.create(new MasterDataRequest("2100-ACCRUAL", "Accrued Liabilities", null), "maker1");
        // 2100-ACCRUAL left PENDING intentionally.

        var usd = currencyService.create(new MasterDataRequest("USD", "US Dollar", null), "maker1");
        currencyService.approve(usd.getId(), "checker1", "Approved for demo use.");
        var eur = currencyService.create(new MasterDataRequest("EUR", "Euro", null), "maker1");
        currencyService.approve(eur.getId(), "checker1", "Approved for demo use.");
        currencyService.create(new MasterDataRequest("GBP", "British Pound", null), "maker1");
        // GBP left PENDING intentionally.

        var acme = counterpartyService.create(new MasterDataRequest("ACME", "Acme Corp", "Vendor"), "maker1");
        counterpartyService.approve(acme.getId(), "checker1", "Approved for demo use.");
        var globex = counterpartyService.create(new MasterDataRequest("GLOBEX", "Globex", "Customer"), "maker1");
        counterpartyService.approve(globex.getId(), "checker1", "Approved for demo use.");
        counterpartyService.create(new MasterDataRequest("INITECH", "Initech", "Customer"), "maker1");
        // Initech left PENDING intentionally.
    }

    private record Line(String amount, int day, String description) {
    }

    private static final List<Line> PRIOR_PERIOD_LINES = List.of(
            new Line("1500.00", 2, "Vendor payment - Acme Corp"),
            new Line("-210.40", 4, "Office supplies"),
            new Line("8600.00", 7, "Customer receipt - Globex"),
            new Line("-75.00", 9, "Bank fee reversal"),
            new Line("2200.00", 11, "Customer receipt - Initech"),
            new Line("-140.60", 14, "Monthly service fee"),
            new Line("960.00", 17, "Vendor refund - Staples"),
            new Line("-320.10", 19, "Courier services"),
            new Line("5400.00", 22, "Customer receipt - Umbrella Corp"),
            new Line("-88.25", 25, "Payroll processing fee")
    );

    private static final List<Line> CURRENT_PERIOD_LINES = List.of(
            new Line("2500.00", 1, "Vendor payment - Acme Corp"),
            new Line("-180.75", 2, "Office supplies"),
            new Line("12500.00", 3, "Customer receipt - Globex"),
            new Line("-450.00", 3, "Bank fee reversal"),
            new Line("3300.50", 4, "Customer receipt - Initech"),
            new Line("-95.25", 5, "Monthly service fee"),
            new Line("1800.00", 5, "Vendor payment - Wayne Enterprises"),
            new Line("-265.00", 6, "Courier services"),
            new Line("6400.00", 7, "Customer receipt - Umbrella Corp"),
            new Line("-120.50", 8, "Payroll processing fee"),
            new Line("940.00", 8, "Vendor refund - Staples"),
            new Line("-310.75", 9, "Equipment lease"),
            new Line("15200.00", 10, "Customer receipt - Stark Industries"),
            new Line("-58.40", 11, "Bank wire fee"),
            new Line("2750.00", 12, "Vendor payment - Oscorp"),
            new Line("-410.20", 13, "Office supplies"),
            new Line("890.00", 13, "Customer receipt - Pied Piper"),
            new Line("-99.99", 14, "Software subscription"),
            new Line("4200.00", 15, "Customer receipt - Hooli"),
            new Line("-145.00", 16, "Courier services"),
            new Line("3100.00", 17, "Vendor payment - Aperture Science"),
            new Line("-72.60", 18, "Bank fee reversal"),
            new Line("7650.00", 19, "Customer receipt - Massive Dynamic"),
            new Line("-233.10", 20, "Payroll processing fee"),
            new Line("1050.00", 21, "Vendor refund - Cyberdyne")
    );

    private void seedBankCashRecords(DataSource internalCash, DataSource bankStatement, String periodCode,
                                      List<Line> matchedPairs, int breakPairCount) {
        YearMonth ym = YearMonth.parse(periodCode);
        IngestionBatch cashBatch = createCompletedBatch(internalCash, "internal_cash_" + periodCode + ".csv");
        IngestionBatch bankBatch = createCompletedBatch(bankStatement, "bank_statement_" + periodCode + ".csv");

        int seq = 2001;
        int bankSeq = 3001;
        for (Line line : matchedPairs) {
            LocalDate date = safeDate(ym, line.day());
            saveRecord(cashBatch, "INTERNAL_CASH", "INT-" + seq, date, new BigDecimal(line.amount()), line.description());
            saveRecord(bankBatch, "BANK_STATEMENT", "BNK-" + bankSeq, date, new BigDecimal(line.amount()), line.description().toUpperCase());
            seq++;
            bankSeq++;
        }

        String[] cashOnlyDescriptions = {
                "In-transit deposit not yet cleared", "Petty cash reimbursement", "Uncleared check #4471", "Manual journal pending bank confirmation",
        };
        String[] bankOnlyDescriptions = {
                "Unrecorded bank service charge", "Unidentified wire credit", "NSF fee", "ATM withdrawal not yet booked",
        };
        for (int i = 0; i < breakPairCount; i++) {
            saveRecord(cashBatch, "INTERNAL_CASH", "INT-" + seq, safeDate(ym, 18 + i), new BigDecimal("100.00").multiply(BigDecimal.valueOf(42 + i)),
                    cashOnlyDescriptions[i % cashOnlyDescriptions.length]);
            saveRecord(bankBatch, "BANK_STATEMENT", "BNK-" + bankSeq, safeDate(ym, 20 + i), new BigDecimal("-10.00").multiply(BigDecimal.valueOf(6 + i)),
                    bankOnlyDescriptions[i % bankOnlyDescriptions.length]);
            seq++;
            bankSeq++;
        }

        int perSide = matchedPairs.size() + breakPairCount;
        cashBatch.setTotalRecords(perSide);
        cashBatch.setSuccessRecords(perSide);
        batchRepository.save(cashBatch);
        bankBatch.setTotalRecords(perSide);
        bankBatch.setSuccessRecords(perSide);
        batchRepository.save(bankBatch);
        log.debug("Seeded {} raw transaction records for period {}.", perSide * 2, periodCode);
    }

    /** GL/Subledger/Intercompany sources have no matching logic wired up yet (see README) - these
     * just give the ingestion batches/records tables realistic content to browse. */
    private void seedOtherSourceRecords(DataSource dataSource, String prefix, String sourceSystem, String periodCode) {
        YearMonth ym = YearMonth.parse(periodCode);
        IngestionBatch batch = createCompletedBatch(dataSource, prefix.toLowerCase() + "demo_" + periodCode + ".csv");
        String[] descriptions = {"Accrued expense", "Intercompany transfer", "Trial balance adjustment", "Elimination entry", "Reclass entry"};
        for (int i = 0; i < 6; i++) {
            saveRecord(batch, sourceSystem, prefix + (9000 + i), safeDate(ym, 2 + i * 4),
                    new BigDecimal("500.00").multiply(BigDecimal.valueOf(i + 1)).negate(), descriptions[i % descriptions.length]);
        }
        batch.setTotalRecords(6);
        batch.setSuccessRecords(6);
        batchRepository.save(batch);
    }

    private void triageCurrentPeriodBreaks(UUID reconciliationId) {
        List<ReconciliationBreak> breaks = breakRepository.findByReconciliationId(reconciliationId);
        if (breaks.size() < 4) {
            return;
        }
        // 2 resolved
        for (int i = 0; i < 2; i++) {
            breakTriageService.assign(breaks.get(i).getId(), "maker1", "admin");
            breakTriageService.resolve(breaks.get(i).getId(), "Confirmed as a timing difference; no adjustment needed.", "maker1");
        }
        // 1 in review (assigned, not yet resolved)
        breakTriageService.assign(breaks.get(2).getId(), "maker1", "admin");
        // 1 escalated
        breakTriageService.escalate(breaks.get(3).getId(), "maker1");
        // remainder left OPEN untouched
    }

    private LocalDate safeDate(YearMonth month, int day) {
        return month.atDay(Math.min(day, month.lengthOfMonth()));
    }

    private IngestionBatch createCompletedBatch(DataSource dataSource, String fileName) {
        IngestionBatch batch = new IngestionBatch();
        batch.setDataSource(dataSource);
        batch.setFileName(fileName);
        batch.setFormat(FileFormat.CSV);
        batch.setStatus(BatchStatus.COMPLETED);
        batch.setStartedAt(Instant.now());
        batch.setCompletedAt(Instant.now());
        return batchRepository.save(batch);
    }

    private void saveRecord(IngestionBatch batch, String sourceSystem, String externalId, LocalDate date,
                             BigDecimal amount, String description) {
        RawTransactionRecord record = new RawTransactionRecord();
        record.setBatch(batch);
        record.setSourceSystem(sourceSystem);
        record.setExternalId(externalId);
        record.setTransactionDate(date);
        record.setAmount(amount);
        record.setCurrency("USD");
        record.setDescription(description);
        recordRepository.save(record);
    }
}
