-- C3: Author yearly linechart by publication type
-- Parameters:
--   1: author_id
--   2: start_year
--   3: end_year

SELECT
    a.year,
    COUNT(DISTINCT a.article_id) AS total_articles,
    COUNT(DISTINCT ja.article_id) AS total_journal_articles,
    COUNT(DISTINCT ca.article_id) AS total_conference_articles
FROM article_authors aa
         JOIN articles a
              ON aa.article_id = a.article_id
         LEFT JOIN journal_articles ja
                   ON a.article_id = ja.article_id
         LEFT JOIN conference_articles ca
                   ON a.article_id = ca.article_id
WHERE aa.author_id = ?
  AND a.year BETWEEN ? AND ?
GROUP BY a.year
ORDER BY a.year;