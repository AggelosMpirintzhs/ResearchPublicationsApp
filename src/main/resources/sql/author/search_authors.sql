-- Fast author search with better priority for initials / prefixes
--
-- Parameters:
--   1: prefix pattern
--   2: word-start pattern
--   3: contains pattern
--   4: prefix pattern
--   5: word-start pattern
--   6: contains pattern
--   7: max results

SELECT
    author_id,
    author_name
FROM authors
WHERE author_name COLLATE utf8mb4_general_ci LIKE ?
   OR author_name COLLATE utf8mb4_general_ci LIKE ?
   OR author_name COLLATE utf8mb4_general_ci LIKE ?
ORDER BY
    CASE
        WHEN author_name COLLATE utf8mb4_general_ci LIKE ? THEN 0
        WHEN author_name COLLATE utf8mb4_general_ci LIKE ? THEN 1
        WHEN author_name COLLATE utf8mb4_general_ci LIKE ? THEN 2
        ELSE 3
        END,
    CHAR_LENGTH(author_name),
    author_name
    LIMIT ?;