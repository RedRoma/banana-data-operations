------------------------------------------------------------------------------
-- REMOVES ALL AN APPLICATION'S OWNERS THAT ARE NOT
-- IN THE SPECIFIED ARRAY
------------------------------------------------------------------------------

DELETE
FROM application_owners
WHERE app_id = ?
AND NOT (owner_id = ANY (string_to_array(?, ',')::UUID[]))
