------------------------------------------------------------------------------
-- GETS THE MESSAGES IN A USER'S INBOX
------------------------------------------------------------------------------

SELECT *
FROM inbox
WHERE user_id = ?