-- C2: Author yearly linechart
-- Parameters:
--   1: author_id
--   2: start_year
--   3: end_year

SELECT
    a.year,
    COUNT(DISTINCT a.article_id) AS total_articles
FROM article_authors aa
         JOIN articles a
              ON aa.article_id = a.article_id
WHERE aa.author_id = ?
  AND a.year BETWEEN ? AND ?
GROUP BY a.year
ORDER BY a.year;