package com.oracle.demo.interactiveai;

import java.util.Collection;
import java.util.Map;

public final class Json {
    private Json() {
    }

    public static String quote(String value) {
        if (value == null) return "null";
        StringBuilder out = new StringBuilder(value.length() + 16).append('"');
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.append('"').toString();
    }

    public static String value(Object value) {
        if (value == null) return "null";
        if (value instanceof String string) return quote(string);
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .map(entry -> quote(String.valueOf(entry.getKey())) + ":" + value(entry.getValue()))
                    .reduce("{", (left, item) -> left.equals("{") ? left + item : left + "," + item) + "}";
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(Json::value).reduce("[", (left, item) -> left.equals("[") ? left + item : left + "," + item) + "]";
        }
        return quote(value.toString());
    }

    public static String account(Account a) {
        return value(Map.of(
                "customerId", a.customerId(), "customerName", a.customerName(),
                "industry", a.industry(), "accountValue", a.accountValue(),
                "riskScore", a.riskScore(), "riskLevel", a.riskLevel(),
                "riskSummary", a.riskSummary(), "ownerName", a.ownerName(),
                "followUpStatus", a.followUpStatus()));
    }
}
