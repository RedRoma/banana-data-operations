------------------------------------------------------------------------------
-- SELECTS ALL A USER'S EVENTS
------------------------------------------------------------------------------

SELECT *
FROM activity
WHERE recipient_user_id = ?