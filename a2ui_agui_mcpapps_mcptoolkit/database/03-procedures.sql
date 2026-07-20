WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

CREATE OR REPLACE PROCEDURE create_customer_follow_up (
    p_customer_id  IN NUMBER,
    p_action_type  IN VARCHAR2,
    p_action_notes IN VARCHAR2,
    p_requested_by IN VARCHAR2,
    p_action_id    OUT NUMBER
) AUTHID DEFINER AS
    v_customer_id customer_accounts.customer_id%TYPE;
BEGIN
    IF p_action_type NOT IN ('REVIEW', 'CONTACT_OWNER', 'FREEZE_CHANGES') THEN
        RAISE_APPLICATION_ERROR(-20001, 'Unsupported action type');
    END IF;

    IF LENGTH(TRIM(p_action_notes)) NOT BETWEEN 3 AND 2000 THEN
        RAISE_APPLICATION_ERROR(-20002, 'Action notes must contain 3 to 2000 characters');
    END IF;

    SELECT customer_id INTO v_customer_id
      FROM customer_accounts
     WHERE customer_id = p_customer_id
       FOR UPDATE;

    INSERT INTO customer_actions (
        customer_id, action_type, action_notes, requested_by, action_status
    ) VALUES (
        p_customer_id, p_action_type, p_action_notes, p_requested_by, 'APPROVED'
    ) RETURNING action_id INTO p_action_id;

    UPDATE customer_accounts
       SET follow_up_status = 'ACTION APPROVED',
           last_updated = SYSTIMESTAMP
     WHERE customer_id = p_customer_id;

    -- The MCP tool owns commit/rollback. Do not commit here.
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20003, 'Customer account not found');
END;
/
