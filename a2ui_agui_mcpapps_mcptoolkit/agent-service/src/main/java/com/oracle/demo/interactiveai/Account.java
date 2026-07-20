package com.oracle.demo.interactiveai;

public record Account(
        long customerId,
        String customerName,
        String industry,
        long accountValue,
        double riskScore,
        String riskLevel,
        String riskSummary,
        String ownerName,
        String followUpStatus) {
}
