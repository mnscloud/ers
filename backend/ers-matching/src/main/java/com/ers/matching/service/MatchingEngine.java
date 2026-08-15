package com.ers.matching.service;

import com.ers.common.enums.ApprovalStatus;
import com.ers.common.enums.AuditAction;
import com.ers.common.enums.MatchType;
import com.ers.common.event.AuditLogEvent;
import com.ers.common.exception.BusinessException;
import com.ers.common.exception.ResourceNotFoundException;
import com.ers.ingestion.domain.RawTransactionRecord;
import com.ers.ingestion.domain.RecordMatchStatus;
import com.ers.ingestion.repository.RawTransactionRecordRepository;
import com.ers.matching.domain.MatchRule;
import com.ers.matching.domain.MatchRun;
import com.ers.matching.domain.MatchedGroup;
import com.ers.matching.domain.RunStatus;
import com.ers.matching.repository.MatchRuleRepository;
import com.ers.matching.repository.MatchRunRepository;
import com.ers.matching.repository.MatchedGroupRepository;
import com.ers.matching.scoring.MatchScorer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Deterministic, rule-based matching for the Bank & Cash case: greedily pairs each source-A record
 * with its best-scoring available source-B candidate (score >= MATCH_THRESHOLD). One-to-many and
 * many-to-many matching are modeled by MatchedGroup already accepting >2 records, but the greedy
 * one-to-one strategy here is the only algorithm implemented so far - a documented follow-up.
 */
@Service
public class MatchingEngine {

    private static final double MATCH_THRESHOLD = 0.85;

    private final MatchRuleRepository matchRuleRepository;
    private final MatchRunRepository matchRunRepository;
    private final MatchedGroupRepository matchedGroupRepository;
    private final RawTransactionRecordRepository recordRepository;
    private final MatchScorer matchScorer;
    private final ApplicationEventPublisher eventPublisher;

    public MatchingEngine(MatchRuleRepository matchRuleRepository,
                           MatchRunRepository matchRunRepository,
                           MatchedGroupRepository matchedGroupRepository,
                           RawTransactionRecordRepository recordRepository,
                           MatchScorer matchScorer,
                           ApplicationEventPublisher eventPublisher) {
        this.matchRuleRepository = matchRuleRepository;
        this.matchRunRepository = matchRunRepository;
        this.matchedGroupRepository = matchedGroupRepository;
        this.recordRepository = recordRepository;
        this.matchScorer = matchScorer;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public MatchRun run(UUID matchRuleId, String periodCode) {
        MatchRule rule = matchRuleRepository.findById(matchRuleId)
                .orElseThrow(() -> ResourceNotFoundException.of("MatchRule", matchRuleId));
        if (!rule.isActive() || rule.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BusinessException("MATCH_RULE_NOT_APPROVED", "Match rule " + rule.getName() + " has not been approved yet");
        }

        MatchRun run = new MatchRun();
        run.setMatchRule(rule);
        run.setPeriodCode(periodCode);
        run.setStatus(RunStatus.RUNNING);
        run.setStartedAt(Instant.now());
        run = matchRunRepository.save(run);

        YearMonth ym = YearMonth.parse(periodCode);
        List<RawTransactionRecord> poolA = new ArrayList<>(recordRepository
                .findBySourceSystemAndMatchStatusAndTransactionDateBetween(
                        rule.getSourceSystemA(), RecordMatchStatus.UNMATCHED, ym.atDay(1), ym.atEndOfMonth()));
        List<RawTransactionRecord> poolB = new ArrayList<>(recordRepository
                .findBySourceSystemAndMatchStatusAndTransactionDateBetween(
                        rule.getSourceSystemB(), RecordMatchStatus.UNMATCHED, ym.atDay(1), ym.atEndOfMonth()));

        int matched = 0;
        for (RawTransactionRecord a : poolA) {
            RawTransactionRecord best = null;
            double bestScore = MATCH_THRESHOLD;
            for (RawTransactionRecord b : poolB) {
                double score = matchScorer.score(a, b, rule);
                if (score >= bestScore) {
                    best = b;
                    bestScore = score;
                }
            }
            if (best != null) {
                MatchedGroup group = new MatchedGroup();
                group.setMatchRun(run);
                group.setMatchType(MatchType.ONE_TO_ONE);
                group.setConfidenceScore(bestScore);
                group.setRecords(Set.of(a, best));
                matchedGroupRepository.save(group);

                a.setMatchStatus(RecordMatchStatus.MATCHED);
                best.setMatchStatus(RecordMatchStatus.MATCHED);
                recordRepository.save(a);
                recordRepository.save(best);

                poolB.remove(best);
                matched++;
            }
        }

        run.setMatchedCount(matched);
        run.setUnmatchedCountA(poolA.size() - matched);
        run.setUnmatchedCountB(poolB.size());
        run.setStatus(RunStatus.COMPLETED);
        run.setCompletedAt(Instant.now());
        MatchRun saved = matchRunRepository.save(run);

        eventPublisher.publishEvent(AuditLogEvent.of(currentUser(), AuditAction.CREATE, "MatchRun",
                saved.getId().toString(), "Match run completed: " + matched + " matched"));

        return saved;
    }

    private String currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
