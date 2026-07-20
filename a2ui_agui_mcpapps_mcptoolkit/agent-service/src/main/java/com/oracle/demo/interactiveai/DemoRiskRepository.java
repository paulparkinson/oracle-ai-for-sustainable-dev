package com.oracle.demo.interactiveai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class DemoRiskRepository implements RiskRepository {
    private static final List<Account> ACCOUNTS = List.of(
            account(1, "Apex Freight Systems", "Logistics", 4_200_000, 96, "CRITICAL", "Payment velocity and beneficiary changes exceed policy.", "Jordan Lee"),
            account(2, "Blue Mesa Energy", "Energy", 8_100_000, 93, "CRITICAL", "Sanctions-screening similarity and unusual cross-border payments.", "Sam Rivera"),
            account(3, "Cobalt Health Partners", "Healthcare", 3_650_000, 91, "CRITICAL", "Repeated access anomalies and overdue compliance evidence.", "Morgan Chen"),
            account(4, "Delta Retail Group", "Retail", 2_850_000, 88, "HIGH", "Chargeback spike and new settlement account.", "Taylor Singh"),
            account(5, "Evergreen Public Works", "Public Sector", 6_400_000, 86, "HIGH", "Contract variance and incomplete ownership attestation.", "Alex Kim"),
            account(6, "Fathom Insurance", "Insurance", 5_100_000, 84, "HIGH", "Claims payout pattern differs from peer baseline.", "Jordan Lee"),
            account(7, "Granite Telecom", "Telecommunications", 4_750_000, 82, "HIGH", "Credential churn and privileged access escalation.", "Sam Rivera"),
            account(8, "Harborline Foods", "Food Distribution", 1_900_000, 79, "HIGH", "Supplier bank change preceded expedited payment.", "Morgan Chen"),
            account(9, "Ionix Manufacturing", "Manufacturing", 7_300_000, 77, "HIGH", "Export-control documentation is incomplete.", "Taylor Singh"),
            account(10, "Juniper Media", "Media", 1_200_000, 74, "HIGH", "Revenue concentration and late covenant reporting.", "Alex Kim")
    );

    private final AtomicLong actionIds = new AtomicLong(1000);
    private final List<ActionResult> actions = new ArrayList<>();

    @Override
    public List<Account> findAtRisk(double minimumRisk, int maximumRows) {
        InputValidation.minimumRisk(minimumRisk);
        InputValidation.maximumRows(maximumRows);
        return ACCOUNTS.stream()
                .filter(account -> account.riskScore() >= minimumRisk)
                .sorted(Comparator.comparingDouble(Account::riskScore).reversed())
                .limit(maximumRows)
                .toList();
    }

    @Override
    public synchronized ActionResult createFollowUp(long customerId, String actionType, String actionNotes, String requestedBy) {
        InputValidation.followUp(customerId, actionType, actionNotes, requestedBy);
        if (ACCOUNTS.stream().noneMatch(account -> account.customerId() == customerId)) {
            throw new IllegalArgumentException("Customer account not found");
        }
        var result = new ActionResult(actionIds.incrementAndGet(), customerId, actionType, "APPROVED");
        actions.add(result);
        return result;
    }

    @Override
    public synchronized int actionCount() {
        return actions.size();
    }

    private static Account account(long id, String name, String industry, long value, double score, String level, String summary, String owner) {
        return new Account(id, name, industry, value, score, level, summary, owner, "NONE");
    }
}
