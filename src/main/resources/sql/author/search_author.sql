-- Helper: Search authors for UI autocomplete / selection
-- Parameters:
--   1: search text
--   2: max results

SELECT
    author_id,
    author_name
FROM authors
WHERE author_name LIKE CONCAT('%', ?, '%')
ORDER BY author_name
    LIMIT ?;