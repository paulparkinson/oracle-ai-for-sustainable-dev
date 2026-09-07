package com.oracle.ai.fullstack.runtime;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/** Reads the supply-chain hotspot tables; bounded demo data is an explicit offline fallback. */
@Service
public class SupplyChainSpatialService {
  private static final Pattern SKU = Pattern.compile("\\b(SKU-\\d+)\\b", Pattern.CASE_INSENSITIVE);
  private static final String QUERY = """
      SELECT summary.product_id, summary.primary_region, summary.stockout_probability,
             geo.warehouse_code, warehouse.warehouse_name, geo.latitude, geo.longitude,
             snapshot.hotspot_rank, snapshot.hotspot_score, snapshot.coverage_days,
             snapshot.recommended_role
        FROM sc_inventory_risk_summary summary
        JOIN sc_warehouse_risk_snapshot snapshot ON snapshot.product_id = summary.product_id AND snapshot.active_flag = 'Y'
        JOIN sc_warehouses warehouse ON warehouse.warehouse_id = snapshot.warehouse_id
        JOIN sc_warehouse_geo geo ON geo.warehouse_id = snapshot.warehouse_id
       WHERE summary.product_id = ? AND summary.active_flag = 'Y'
       ORDER BY snapshot.hotspot_rank
      """;
  private final Environment environment;
  public SupplyChainSpatialService(Environment environment) { this.environment = environment; }
  public SpatialResult resolve(String input) {
    String product = productId(input);
    try { return fromDatabase(product); }
    catch (Exception exception) { return demo(product, exception.getMessage()); }
  }
  private SpatialResult fromDatabase(String product) throws Exception {
    try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(QUERY)) {
      statement.setString(1, product);
      try (ResultSet rows = statement.executeQuery()) {
        var hotspots = new java.util.ArrayList<Hotspot>(); String region = null; double stockout = 0;
        while (rows.next()) { region = rows.getString("primary_region"); stockout = rows.getDouble("stockout_probability"); hotspots.add(new Hotspot(rows.getString("warehouse_code"), rows.getString("warehouse_name"), rows.getDouble("latitude"), rows.getDouble("longitude"), rows.getInt("hotspot_rank"), rows.getDouble("hotspot_score"), rows.getDouble("coverage_days"), rows.getString("recommended_role"))); }
        if (hotspots.isEmpty()) throw new IllegalArgumentException("No active spatial rows for " + product);
        return new SpatialResult(product, region, stockout, List.copyOf(hotspots), "oracle-database", "Oracle supply-chain hotspot tables");
      }
    }
  }
  private Connection connection() throws Exception {
    String username = required("DB_USERNAME"), password = required("DB_PASSWORD"), dsn = required("DB_DSN"), tns = first("TNS_ADMIN", "DB_WALLET_DIR");
    if (tns.isBlank()) throw new IllegalArgumentException("TNS_ADMIN or DB_WALLET_DIR is required");
    Class.forName("oracle.jdbc.OracleDriver");
    Properties p = new Properties(); p.setProperty("user", username); p.setProperty("password", password); p.setProperty("oracle.net.tns_admin", tns); p.setProperty("oracle.net.ssl_server_dn_match", "false");
    return DriverManager.getConnection("jdbc:oracle:thin:@" + dsn + "?TNS_ADMIN=" + tns, p);
  }
  private SpatialResult demo(String product, String reason) {
    return new SpatialResult(product, "Northeast", .82, List.of(
      new Hotspot("EWR", "Newark DC", 40.7357, -74.1724, 1, 92, 3.0, "DESTINATION - expedite relief"),
      new Hotspot("DFW", "Dallas-Fort Worth DC", 32.8998, -97.0403, 2, 34, 18.0, "SOURCE - transfer 500 units"),
      new Hotspot("RNO", "Reno DC", 39.5296, -119.8138, 3, 48, 12.0, "MONITOR")), "seeded-demo", "Database unavailable: " + reason);
  }
  private String productId(String input) { Matcher m = SKU.matcher(input == null ? "" : input.toUpperCase(Locale.ROOT)); return m.find() ? m.group(1) : "SKU-500"; }
  private String required(String name) { String value = environment.getProperty(name); if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required"); return value.trim(); }
  private String first(String... names) { for (String name : names) { String value = environment.getProperty(name); if (value != null && !value.isBlank()) return value.trim(); } return ""; }
  public record SpatialResult(String productId, String region, double stockoutProbability, List<Hotspot> hotspots, String sourceMode, String sourceDetail) {}
  public record Hotspot(String code, String name, double latitude, double longitude, int rank, double score, double coverageDays, String recommendedRole) {}
}
