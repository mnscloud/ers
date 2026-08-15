package com.ers.web.dashboard;

import com.ers.adjustment.domain.JournalEntryStatus;
import com.ers.adjustment.repository.JournalEntryRepository;
import com.ers.common.web.ApiResponse;
import com.ers.exception.domain.BreakStatus;
import com.ers.exception.repository.ReconciliationBreakRepository;
import com.ers.reconciliation.domain.ReconStatus;
import com.ers.reconciliation.repository.ReconciliationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ReconciliationRepository reconciliationRepository;
    private final ReconciliationBreakRepository breakRepository;
    private final JournalEntryRepository journalEntryRepository;

    public DashboardController(ReconciliationRepository reconciliationRepository,
                                ReconciliationBreakRepository breakRepository,
                                JournalEntryRepository journalEntryRepository) {
        this.reconciliationRepository = reconciliationRepository;
        this.breakRepository = breakRepository;
        this.journalEntryRepository = journalEntryRepository;
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> summary() {
        Map<String, Long> reconciliations = new LinkedHashMap<>();
        for (ReconStatus status : ReconStatus.values()) {
            reconciliations.put(status.name(), reconciliationRepository.countByStatus(status));
        }

        Map<String, Long> breaks = new LinkedHashMap<>();
        for (BreakStatus status : BreakStatus.values()) {
            breaks.put(status.name(), breakRepository.countByStatus(status));
        }

        Map<String, Long> journalEntries = new LinkedHashMap<>();
        for (JournalEntryStatus status : JournalEntryStatus.values()) {
            journalEntries.put(status.name(), journalEntryRepository.countByStatus(status));
        }

        return ApiResponse.ok(new DashboardSummaryResponse(reconciliations, breaks, journalEntries));
    }
}
