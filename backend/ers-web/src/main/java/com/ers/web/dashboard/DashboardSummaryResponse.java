package com.ers.web.dashboard;

import java.util.Map;

public record DashboardSummaryResponse(
        Map<String, Long> reconciliationsByStatus,
        Map<String, Long> breaksByStatus,
        Map<String, Long> journalEntriesByStatus
) {
}
