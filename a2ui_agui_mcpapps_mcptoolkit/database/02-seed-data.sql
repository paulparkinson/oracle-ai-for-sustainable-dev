WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

INSERT INTO customer_accounts (customer_name, industry, account_value, risk_score, risk_level, risk_summary, owner_name) VALUES ('Apex Freight Systems', 'Logistics', 4200000, 96, 'CRITICAL', 'Payment velocity and beneficiary changes exceed policy.', 'Jordan Lee');
INSERT INTO customer_accounts (customer_name, industry, account_value, risk_score, risk_level, risk_summary, owner_name) VALUES ('Blue Mesa Energy', 'Energy', 8100000, 93, 'CRITICAL', 'Sanctions-screening similarity and unusual cross-border payments.', 'Sam Rivera');
INSERT INTO customer_accounts (customer_name, industry, account_value, risk_score, risk_level, risk_summary, owner_name) VALUES ('Cobalt Health Partners', 'Healthcare', 3650000, 91, 'CRITICAL', 'Repeated access anomalies and overdue compliance evidence.', 'Morgan Chen');
INSERT INTO customer_accounts (customer_name, industry, account_value, risk_score, risk_level, risk_summary, owner_name) VALUES ('Delta Retail Group', 'Retail', 2850000, 88, 'HIGH', 'Chargeback spike and new settlement account.', 'Taylor Singh');
INSERT INTO customer_accounts (customer_name, industry, account_value, risk_score, risk_level, risk_summary, owner_name) VALUES ('Evergreen Public Works', 'Public Sector', 6400000, 86, 'HIGH', 'Contract variance and incomplete ownership attestation.', 'Alex Kim');
INSERT INTO customer_accounts (customer_name, industry, account_value, risk_score, risk_level, risk_summary, owner_name) VALUES ('Fathom Insurance', 'Insurance', 5100000, 84, 'HIGH', 'Claims payout pattern differs from peer baseline.', 'Jordan Lee');
INSERT INTO customer_accounts (customer_name, industry, account_value, risk_score, risk_level, risk_summary, owner_name) VALUES ('Granite Telecom', 'Telecommunications', 4750000, 82, 'HIGH', 'Credential churn and privileged access escalation.', 'Sam Rivera');
INSERT INTO customer_accounts (customer_name, industry, account_value, risk_score, risk_level, risk_summary, owner_name) VALUES ('Harborline Foods', 'Food Distribution', 1900000, 79, 'HIGH', 'Supplier bank change preceded expedited payment.', 'Morgan Chen');
INSERT INTO customer_accounts (customer_name, industry, account_value, risk_score, risk_level, risk_summary, owner_name) VALUES ('Ionix Manufacturing', 'Manufacturing', 7300000, 77, 'HIGH', 'Export-control documentation is incomplete.', 'Taylor Singh');
INSERT INTO customer_accounts (customer_name, industry, account_value, risk_score, risk_level, risk_summary, owner_name) VALUES ('Juniper Media', 'Media', 1200000, 74, 'HIGH', 'Revenue concentration and late covenant reporting.', 'Alex Kim');
INSERT INTO customer_accounts (customer_name, industry, account_value, risk_score, risk_level, risk_summary, owner_name) VALUES ('Keystone Aviation', 'Aviation', 9200000, 69, 'MEDIUM', 'Maintenance reserve variance requires review.', 'Jordan Lee');
INSERT INTO customer_accounts (customer_name, industry, account_value, risk_score, risk_level, risk_summary, owner_name) VALUES ('Lumen Education', 'Education', 2100000, 66, 'MEDIUM', 'Enrollment forecast diverges from observed receipts.', 'Sam Rivera');
INSERT INTO customer_accounts (customer_name, industry, account_value, risk_score, risk_level, risk_summary, owner_name) VALUES ('Meridian BioLabs', 'Life Sciences', 5800000, 63, 'MEDIUM', 'Trial milestone delay may affect liquidity.', 'Morgan Chen');
INSERT INTO customer_accounts (customer_name, industry, account_value, risk_score, risk_level, risk_summary, owner_name) VALUES ('Northstar Hospitality', 'Hospitality', 3300000, 61, 'MEDIUM', 'Seasonal cash coverage below internal target.', 'Taylor Singh');
INSERT INTO customer_accounts (customer_name, industry, account_value, risk_score, risk_level, risk_summary, owner_name) VALUES ('Orchid Software', 'Technology', 2700000, 58, 'MEDIUM', 'Customer concentration increased this quarter.', 'Alex Kim');
INSERT INTO customer_accounts (customer_name, industry, account_value, risk_score, risk_level, risk_summary, owner_name) VALUES ('Pioneer Construction', 'Construction', 4500000, 54, 'MEDIUM', 'Two projects show schedule and cost pressure.', 'Jordan Lee');
INSERT INTO customer_accounts (customer_name, industry, account_value, risk_score, risk_level, risk_summary, owner_name) VALUES ('Quartz Mobility', 'Transportation', 3900000, 47, 'LOW', 'Insurance renewal is pending but within tolerance.', 'Sam Rivera');
INSERT INTO customer_accounts (customer_name, industry, account_value, risk_score, risk_level, risk_summary, owner_name) VALUES ('Redwood Agriculture', 'Agriculture', 2400000, 42, 'LOW', 'Commodity exposure is hedged within policy.', 'Morgan Chen');
INSERT INTO customer_accounts (customer_name, industry, account_value, risk_score, risk_level, risk_summary, owner_name) VALUES ('Solstice Apparel', 'Apparel', 1750000, 36, 'LOW', 'Minor forecast variance with adequate liquidity.', 'Taylor Singh');
INSERT INTO customer_accounts (customer_name, industry, account_value, risk_score, risk_level, risk_summary, owner_name) VALUES ('Trellis Legal Services', 'Professional Services', 980000, 28, 'LOW', 'No material exceptions; annual review upcoming.', 'Alex Kim');

INSERT INTO customer_risk_events (customer_id, event_type, event_description, event_date, severity)
SELECT customer_id, 'PAYMENT_ANOMALY', 'Payment velocity exceeded the 30-day account baseline.', SYSTIMESTAMP - INTERVAL '2' DAY, LEAST(10, CEIL(risk_score / 10))
FROM customer_accounts;

INSERT INTO customer_risk_events (customer_id, event_type, event_description, event_date, severity)
SELECT customer_id, 'CONTROL_REVIEW', 'Automated control review recorded the current account risk classification.', SYSTIMESTAMP - INTERVAL '9' DAY, GREATEST(1, CEIL(risk_score / 12))
FROM customer_accounts;

COMMIT;
