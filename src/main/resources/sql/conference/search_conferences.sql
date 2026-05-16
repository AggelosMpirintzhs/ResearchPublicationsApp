-- Helper: Search conferences for UI autocomplete / selection
-- Parameters:
--   1: search text
--   2: search text
--   3: max results

SELECT
    conference_id,
    acronym,
    title AS conference_title,
    icore_id
FROM conferences
WHERE acronym LIKE CONCAT('%', ?, '%')
   OR title LIKE CONCAT('%', ?, '%')
ORDER BY acronym, title
    LIMIT ?;