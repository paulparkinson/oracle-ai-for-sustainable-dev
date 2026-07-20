package com.oracle.demo.interactiveai;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ApprovalService {
    private final Map<String, PendingApproval> pending = new ConcurrentHashMap<>();
    private final Clock clock;

    public ApprovalService() {
        this(Clock.systemUTC());
    }

    ApprovalService(Clock clock) {
        this.clock = clock;
    }

    public String issue(String actor, List<Account> accounts) {
        String id = UUID.randomUUID().toString();
        pending.put(id, new PendingApproval(actor, accounts.stream().map(Account::customerId).toList(), clock.instant().plus(Duration.ofMinutes(10))));
        return id;
    }

    public PendingApproval consume(String approvalId, long customerId, String actor) {
        PendingApproval approval = pending.remove(approvalId);
        if (approval == null) throw new IllegalArgumentException("Approval is missing, expired, or already used");
        if (clock.instant().isAfter(approval.expiresAt())) throw new IllegalArgumentException("Approval has expired");
        if (!approval.actor().equals(actor)) throw new IllegalArgumentException("Approval actor does not match");
        if (!approval.customerIds().contains(customerId)) throw new IllegalArgumentException("Account was not part of the approved review set");
        return approval;
    }

    public void reject(String approvalId, String actor) {
        PendingApproval approval = pending.remove(approvalId);
        if (approval == null || !approval.actor().equals(actor)) {
            throw new IllegalArgumentException("Approval is missing or actor does not match");
        }
    }

    public record PendingApproval(String actor, List<Long> customerIds, Instant expiresAt) {
    }
}
