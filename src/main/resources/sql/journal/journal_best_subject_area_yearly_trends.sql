SELECT
    b.area_name AS category,
    a.year AS year,
    COUNT(DISTINCT j.journal_id) AS count
FROM journal_rankings jr
    JOIN best_subject_areas b
ON b.best_area_id = jr.best_area_id
    JOIN journals j
    ON j.journal_id = jr.journal_id
    JOIN journal_articles ja
    ON ja.journal_id = j.journal_id
    JOIN articles a
    ON a.article_id = ja.article_id
WHERE
    (? = '' OR CAST(jr.best_area_id AS CHAR) = ?)
  AND a.year BETWEEN ? AND ?
GROUP BY
    b.area_name,
    a.year
ORDER BY
    a.year ASC,
    count DESC,
    b.area_name ASC;