package oracleai;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class SelectAiService {

    private static final Pattern PRODUCT_ID_PATTERN = Pattern.compile("\\b([A-Z]{2,}-\\d+)\\b");
    private static final String GENERATE_SQL =
            "SELECT DBMS_CLOUD_AI.GENERATE(prompt => ?, profile_name => ?, action => ?) AS ai_response FROM dual";
    private static final String SUMMARY_SQL = """
            SELECT
                product_id,
                product_name,
                quarter_label,
                risk_level,
                stockout_probability,
                at_risk_units,
                projected_revenue_impact_usd,
                primary_region,
                recommendation_summary
            FROM sc_inventory_risk_summary
            ORDER BY stockout_probability DESC
            FETCH FIRST 3 ROWS ONLY
            """;
    private static final String DRIVER_SQL = """
            SELECT
                product_id,
                warehouse_name,
                county_name,
                state_code,
                hotspot_rank,
                hotspot_score,
                coverage_days,
                revenue_impact_usd
            FROM sc_inventory_risk_demo_v
            WHERE product_id = ?
            ORDER BY hotspot_rank
            """;

    private final Environment environment;

    public SelectAiService(Environment environment) {
        this.environment = environment;
    }

    public SelectAiResult answer(String userInput) {
        String normalizedPrompt = normalizePrompt(userInput);
        String action = determineAction(normalizedPrompt);
        String productId = extractProductId(normalizedPrompt);
        String profileName = firstNonBlank(
                environment.getProperty("SELECT_AI_PROFILE"),
                environment.getProperty("DBMS_CLOUD_AI_PROFILE")
        );

        if (!profileName.isBlank()) {
            try {
                return runSelectAi(normalizedPrompt, productId, action, profileName);
            } catch (Exception exception) {
                return fallbackResponse(normalizedPrompt, productId, action, exception.getMessage());
            }
        }

        return fallbackResponse(normalizedPrompt, productId, action, "SELECT_AI_PROFILE is not configured");
    }

    private SelectAiResult runSelectAi(String prompt, String productId, String action, String profileName) throws Exception {
        if ("showsql".equals(action) || "explainsql".equals(action)) {
            return new SelectAiResult(
                    runSelectAiPrompt(prompt, action, profileName),
                    "select-ai",
                    action,
                    "DBMS_CLOUD_AI.GENERATE via profile " + profileName
            );
        }

        String lower = prompt.toLowerCase(Locale.ROOT);
        boolean wantsRiskSummary = lower.contains("product")
                || lower.contains("stockout")
                || lower.contains("next quarter")
                || lower.contains("risk");
        boolean wantsRegionDrivers = asksForRegions(lower);

        if (wantsRiskSummary && wantsRegionDrivers) {
            String summaryText = runSelectAiPrompt(buildRiskSummaryPrompt(), action, profileName);
            String regionText = runSelectAiPrompt(buildRegionDriverPrompt(productId), action, profileName);
            return new SelectAiResult(
                    summaryText + "\n\n" + regionText,
                    "select-ai",
                    action,
                    "DBMS_CLOUD_AI.GENERATE via profile " + profileName
            );
        }

        if (wantsRegionDrivers) {
            return new SelectAiResult(
                    runSelectAiPrompt(buildRegionDriverPrompt(productId), action, profileName),
                    "select-ai",
                    action,
                    "DBMS_CLOUD_AI.GENERATE via profile " + profileName
            );
        }

        if (wantsRiskSummary) {
            return new SelectAiResult(
                    runSelectAiPrompt(buildRiskSummaryPrompt(), action, profileName),
                    "select-ai",
                    action,
                    "DBMS_CLOUD_AI.GENERATE via profile " + profileName
            );
        }

        return new SelectAiResult(
                runSelectAiPrompt(prompt, action, profileName),
                "select-ai",
                action,
                "DBMS_CLOUD_AI.GENERATE via profile " + profileName
        );
    }

    private String runSelectAiPrompt(String prompt, String action, String profileName) throws Exception {
        try (
                Connection connection = OracleJdbcSupport.openConnection(environment);
                PreparedStatement statement = connection.prepareStatement(GENERATE_SQL)
        ) {
            statement.setString(1, prompt);
            statement.setString(2, profileName);
            statement.setString(3, action);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("DBMS_CLOUD_AI.GENERATE returned no rows.");
                }

                String content = resultSet.getString("ai_response");
                if (content == null || content.isBlank()) {
                    throw new IllegalStateException("DBMS_CLOUD_AI.GENERATE returned an empty response.");
                }
                return content.trim();
            }
        }
    }

    private String buildRiskSummaryPrompt() {
        String quarterLabel = resolveQuarterLabel();
        return "Using ADMIN.SC_INVENTORY_RISK_SUMMARY joined to ADMIN.SC_PRODUCTS, summarize the products at highest "
                + "stockout risk for quarter label " + quarterLabel + ". Include product_id, product_name, risk_level, "
                + "stockout_probability, projected_revenue_impact_usd, and primary_region.";
    }

    private String buildRegionDriverPrompt(String productId) {
        return "Using ADMIN.SC_INVENTORY_RISK_DEMO_V, explain which warehouses, counties, states, hotspot scores, "
                + "and coverage days are driving the risk for product " + productId + ".";
    }

    private String resolveQuarterLabel() {
        try (
                Connection connection = OracleJdbcSupport.openConnection(environment);
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT quarter_label FROM sc_inventory_risk_summary WHERE active_flag = 'Y' FETCH FIRST 1 ROWS ONLY"
                );
                ResultSet resultSet = statement.executeQuery()
        ) {
            if (resultSet.next()) {
                String quarterLabel = resultSet.getString(1);
                if (quarterLabel != null && !quarterLabel.isBlank()) {
                    return quarterLabel.trim();
                }
            }
        } catch (Exception ignored) {
            // Fall back to the seeded demo label below.
        }
        return DemoInventoryData.topSummaries().get(0).quarterLabel();
    }

    private SelectAiResult fallbackResponse(String prompt, String productId, String action, String reason) {
        String lower = prompt.toLowerCase(Locale.ROOT);
        boolean wantsRiskSummary = lower.contains("product")
                || lower.contains("stockout")
                || lower.contains("next quarter")
                || lower.contains("risk");
        boolean wantsRegionDrivers = asksForRegions(lower);

        if ("showsql".equals(action)) {
            String sql = DemoInventoryData.fallbackShowSql(productId, asksForRegions(lower));
            return new SelectAiResult(
                    "Fallback SQL for the current demo tables:\n\n" + sql
                            + "\nThe live Select AI profile was unavailable, so this is a deterministic SQL fallback ("
                            + reason + ").",
                    "direct-sql-fallback",
                    action,
                    "Deterministic SQL fallback"
            );
        }

        if (wantsRiskSummary && wantsRegionDrivers) {
            return new SelectAiResult(
                    buildRiskNarrative(reason, false) + "\n\n" + buildRegionDriverNarrative(productId, reason, false)
                            + "\n\n"
                            + buildFallbackFooter(reason),
                    "direct-sql-fallback",
                    action,
                    "Direct Oracle SQL summary over demo tables"
            );
        }

        if (wantsRegionDrivers) {
            return new SelectAiResult(
                    buildRegionDriverNarrative(productId, reason, true),
                    "direct-sql-fallback",
                    action,
                    "Direct Oracle SQL summary over demo tables"
            );
        }

        return new SelectAiResult(
                buildRiskNarrative(reason, true) + "\n" + buildFallbackFooter(reason),
                "direct-sql-fallback",
                action,
                "Direct Oracle SQL summary over demo tables"
        );
    }

    private String buildRiskNarrative(String reason, boolean includeFooter) {
        List<DemoInventoryData.InventoryRiskSummary> summaries = new ArrayList<>();
        try (
                Connection connection = OracleJdbcSupport.openConnection(environment);
                PreparedStatement statement = connection.prepareStatement(SUMMARY_SQL);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                summaries.add(new DemoInventoryData.InventoryRiskSummary(
                        resultSet.getString("product_id"),
                        resultSet.getString("product_name"),
                        resultSet.getString("quarter_label"),
                        resultSet.getString("risk_level"),
                        resultSet.getDouble("stockout_probability"),
                        resultSet.getInt("at_risk_units"),
                        resultSet.getInt("projected_revenue_impact_usd"),
                        resultSet.getString("primary_region"),
                        resultSet.getString("recommendation_summary")
                ));
            }
        } catch (Exception ignored) {
            summaries = DemoInventoryData.topSummaries();
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Inventory-risk summary for the next quarter:\n");
        for (DemoInventoryData.InventoryRiskSummary summary : summaries) {
            builder.append("- ")
                    .append(summary.productId())
                    .append(" (")
                    .append(summary.productName())
                    .append(") is ")
                    .append(summary.riskLevel())
                    .append(" risk with stockout probability ")
                    .append(String.format("%.0f%%", summary.stockoutProbability() * 100.0))
                    .append(", at-risk units ")
                    .append(summary.atRiskUnits())
                    .append(", projected revenue impact $")
                    .append(String.format("%,d", summary.projectedRevenueImpactUsd()))
                    .append(", driven mainly by ")
                    .append(summary.primaryRegion())
                    .append(".\n");
        }
        if (includeFooter) {
            builder.append(buildFallbackFooter(reason));
        }
        return builder.toString().trim();
    }

    private String buildRegionDriverNarrative(String productId, String reason, boolean includeFooter) {
        List<RegionDriverRow> rows = new ArrayList<>();
        try (
                Connection connection = OracleJdbcSupport.openConnection(environment);
                PreparedStatement statement = connection.prepareStatement(DRIVER_SQL)
        ) {
            statement.setString(1, productId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new RegionDriverRow(
                            resultSet.getString("product_id"),
                            resultSet.getString("warehouse_name"),
                            resultSet.getString("county_name"),
                            resultSet.getString("state_code"),
                            resultSet.getInt("hotspot_rank"),
                            resultSet.getDouble("hotspot_score"),
                            resultSet.getDouble("coverage_days"),
                            resultSet.getDouble("revenue_impact_usd")
                    ));
                }
            }
        } catch (Exception ignored) {
            for (DemoInventoryData.WarehouseHotspot hotspot : DemoInventoryData.hotspotsFor(productId)) {
                rows.add(new RegionDriverRow(
                        hotspot.productId(),
                        hotspot.warehouseName(),
                        hotspot.countyName(),
                        hotspot.stateCode(),
                        hotspot.hotspotRank(),
                        hotspot.hotspotScore(),
                        hotspot.coverageDays(),
                        hotspot.revenueImpactUsd()
                ));
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Regional drivers for ").append(productId).append(":\n");
        for (RegionDriverRow row : rows) {
            builder.append("- Rank ")
                    .append(row.hotspotRank())
                    .append(": ")
                    .append(row.warehouseName())
                    .append(" in ")
                    .append(row.countyName())
                    .append(" County, ")
                    .append(row.stateCode())
                    .append(" has hotspot score ")
                    .append(String.format("%.2f", row.hotspotScore()))
                    .append(", coverage ")
                    .append(String.format("%.1f", row.coverageDays()))
                    .append(" days, and revenue impact about $")
                    .append(String.format("%,.0f", row.revenueImpactUsd()))
                    .append(".\n");
        }
        if (includeFooter) {
            builder.append(buildFallbackFooter(reason));
        }
        return builder.toString().trim();
    }

    private String buildFallbackFooter(String reason) {
        return "This used a deterministic fallback over the demo tables because the live Select AI path was unavailable ("
                + reason
                + ").";
    }

    private static String normalizePrompt(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return "Which products are at risk of stockouts next quarter?";
        }
        return userInput.trim();
    }

    private static boolean asksForRegions(String prompt) {
        return prompt.contains("region")
                || prompt.contains("warehouse")
                || prompt.contains("county")
                || prompt.contains("map")
                || prompt.contains("driving");
    }

    private static String determineAction(String prompt) {
        String lower = prompt.toLowerCase(Locale.ROOT);
        if (lower.contains("show sql") || lower.contains("showsql")) {
            return "showsql";
        }
        if (lower.contains("explain sql") || lower.contains("explainsql")) {
            return "explainsql";
        }
        if (lower.contains("chat")) {
            return "chat";
        }
        return "narrate";
    }

    private static String extractProductId(String prompt) {
        Matcher matcher = PRODUCT_ID_PATTERN.matcher(prompt == null ? "" : prompt.toUpperCase(Locale.ROOT));
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "SKU-500";
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    public record SelectAiResult(
            String responseText,
            String executionMode,
            String action,
            String sourceDetail
    ) {}

    private record RegionDriverRow(
            String productId,
            String warehouseName,
            String countyName,
            String stateCode,
            int hotspotRank,
            double hotspotScore,
            double coverageDays,
            double revenueImpactUsd
    ) {}
}
