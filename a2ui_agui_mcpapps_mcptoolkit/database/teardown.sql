WHENEVER SQLERROR CONTINUE
DROP PROCEDURE create_customer_follow_up_mcp;
DROP PROCEDURE create_customer_follow_up;
DROP SEQUENCE customer_action_mcp_seq;
DROP VIEW account_risk_event_v;
DROP VIEW account_risk_summary_v;
DROP TABLE customer_actions PURGE;
DROP TABLE customer_risk_events PURGE;
DROP TABLE customer_accounts PURGE;
