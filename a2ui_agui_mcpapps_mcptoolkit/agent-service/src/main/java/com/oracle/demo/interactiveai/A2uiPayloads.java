package com.oracle.demo.interactiveai;

import java.util.List;
import java.util.Map;

public final class A2uiPayloads {
    public static final String VERSION = "v0.9.1";
    public static final String CATALOG = "https://a2ui.org/specification/v0_9_1/catalogs/basic/catalog.json";

    private A2uiPayloads() {
    }

    public static Map<String, Object> createSurface() {
        return Map.of("version", VERSION, "createSurface", Map.of(
                "surfaceId", "account-risk-review",
                "catalogId", CATALOG,
                "sendDataModel", true,
                "theme", Map.of("primaryColor", "#c74634")));
    }

    public static Map<String, Object> components() {
        List<Map<String, Object>> components = List.of(
                Map.of("id", "root", "component", "Column", "children", List.of("banner", "accountList", "actionType", "notes", "buttons")),
                Map.of("id", "banner", "component", "Text", "text", Map.of("path", "/summary")),
                Map.of("id", "accountList", "component", "List", "children", Map.of("path", "/accounts", "componentId", "accountCard")),
                Map.of("id", "accountCard", "component", "Card", "child", "accountText"),
                Map.of("id", "accountText", "component", "Text", "text", Map.of("call", "formatString", "args", Map.of("value", "${customerName} — ${riskLevel} (${riskScore})"))),
                Map.of("id", "actionType", "component", "ChoicePicker", "label", "Follow-up action", "value", Map.of("path", "/form/actionType"), "options", List.of(
                        Map.of("label", "Review", "value", "REVIEW"),
                        Map.of("label", "Contact owner", "value", "CONTACT_OWNER"),
                        Map.of("label", "Freeze changes", "value", "FREEZE_CHANGES"))),
                Map.of("id", "notes", "component", "TextField", "label", "Approval notes", "value", Map.of("path", "/form/actionNotes")),
                Map.of("id", "buttons", "component", "Row", "children", List.of("confirm", "cancel")),
                Map.of("id", "confirm", "component", "Button", "text", "Approve follow-up", "variant", "primary", "action", Map.of("event", Map.of("name", "approve_follow_up"))),
                Map.of("id", "cancel", "component", "Button", "text", "Cancel", "variant", "borderless", "action", Map.of("event", Map.of("name", "reject_follow_up")))
        );
        return Map.of("version", VERSION, "updateComponents", Map.of("surfaceId", "account-risk-review", "components", components));
    }

    public static Map<String, Object> data(List<Account> accounts, String approvalId) {
        List<Map<String, Object>> rows = accounts.stream().map(a -> Map.<String, Object>of(
                "customerId", a.customerId(), "customerName", a.customerName(),
                "industry", a.industry(), "accountValue", a.accountValue(),
                "riskScore", a.riskScore(), "riskLevel", a.riskLevel(),
                "riskSummary", a.riskSummary(), "ownerName", a.ownerName())).toList();
        return Map.of("version", VERSION, "updateDataModel", Map.of(
                "surfaceId", "account-risk-review", "path", "/", "value", Map.of(
                        "summary", accounts.size() + " governed account(s) require review.",
                        "accounts", rows,
                        "approvalId", approvalId,
                        "form", Map.of("actionType", "REVIEW", "actionNotes", "Review risk evidence with the account owner."))));
    }
}
