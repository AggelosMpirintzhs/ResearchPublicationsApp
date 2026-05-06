-- Helper: Search journals for UI autocomplete / selection
-- Parameters:
--   1: search text
--   2: max results

SELECT
    j.journal_id,
    j.journal_name,
    j.publisher_id,
    p.publisher_name
FROM journals j
         LEFT JOIN publishers p
                   ON j.publisher_id = p.publisher_id
WHERE j.journal_name LIKE CONCAT('%', ?, '%')
ORDER BY j.journal_name
    LIMIT ?;