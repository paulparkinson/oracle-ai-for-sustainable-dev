package com.oracle.demo.interactiveai;

import java.util.List;

public interface RiskRepository {
    List<Account> findAtRisk(double minimumRisk, int maximumRows);

    ActionResult createFollowUp(long customerId, String actionType, String actionNotes, String requestedBy);

    int actionCount();
}
