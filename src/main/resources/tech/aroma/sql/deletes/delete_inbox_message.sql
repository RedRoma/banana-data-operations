------------------------------------------------------------------------------
-- DELETES A MESSAGE FROM AN INBOX
------------------------------------------------------------------------------

DELETE
FROM inbox
WHERE app_id = ?
      AND message_id = ?