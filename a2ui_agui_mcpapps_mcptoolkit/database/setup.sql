WHENEVER SQLERROR EXIT SQL.SQLCODE

PROMPT Creating account-risk schema objects...
@@01-schema.sql
@@02-seed-data.sql
@@03-procedures.sql
@@04-views.sql

PROMPT Account-risk database setup complete.
