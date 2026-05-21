SELECT
    p.publisher_id,
    p.publisher_name,
    COUNT(DISTINCT ja.article_id) AS total_publications
FROM publishers p
         JOIN journals j
              ON j.publisher_id = p.publisher_id
         JOIN journal_articles ja
              ON ja.journal_id = j.journal_id
WHERE p.publisher_name IS NOT NULL
  AND TRIM(p.publisher_name) <> ''
  AND (
    ? IS NULL
        OR ? = ''
        OR LOWER(p.publisher_name) LIKE CONCAT('%', LOWER(?), '%')
    )
GROUP BY
    p.publisher_id,
    p.publisher_name
HAVING COUNT(DISTINCT ja.article_id) > 0
ORDER BY
    total_publications DESC,
    p.publisher_name ASC
    LIMIT ?;