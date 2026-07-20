package com.oracle.demo.interactiveai;

public record ActionResult(long actionId, long customerId, String actionType, String status) {
}
