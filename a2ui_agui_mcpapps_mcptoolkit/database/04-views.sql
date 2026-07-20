WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

CREATE OR REPLACE VIEW account_risk_summary_v AS
SELECT customer_id,
       customer_name,
       industry,
       account_value,
       risk_score,
       risk_level,
       risk_summary,
       owner_name,
       follow_up_status,
       last_updated
  FROM customer_accounts;

CREATE OR REPLACE VIEW account_risk_event_v AS
SELECT e.event_id,
       e.customer_id,
       a.customer_name,
       e.event_type,
       e.event_description,
       e.event_date,
       e.severity
  FROM customer_risk_events e
  JOIN customer_accounts a ON a.customer_id = e.customer_id;
