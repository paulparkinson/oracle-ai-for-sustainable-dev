-- Run once as ADMIN on paulparkdb before completing the FINANCIAL-owned demo.
-- These grants are deliberately limited to the three capabilities used by the
-- property-graph and Select AI agent setup.

set echo on feedback on serveroutput on
whenever sqlerror exit sql.sqlcode rollback

begin
    if sys_context('USERENV', 'SESSION_USER') <> 'ADMIN' then
        raise_application_error(-20001, 'Connect as ADMIN before running this script.');
    end if;
end;
/

grant create property graph to FINANCIAL;
grant execute on dbms_cloud_ai to FINANCIAL;
grant execute on dbms_cloud_ai_agent to FINANCIAL;

begin
    dbms_network_acl_admin.append_host_ace(
        host => 'generativelanguage.googleapis.com',
        ace  => xs$ace_type(
            privilege_list => xs$name_list('http'),
            principal_name => 'FINANCIAL',
            principal_type => xs_acl.ptype_db
        )
    );
end;
/

prompt ADMIN preparation complete for FINANCIAL.
exit success
