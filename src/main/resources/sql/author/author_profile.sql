-- C1: Author profile
-- Parameters:
--   1: author_id
--   2: start_year
--   3: end_year

SELECT
    au.author_id,
    au.author_name,

    MIN(a.year) AS first_year,
    MAX(a.year) AS last_year,
    COUNT(DISTINCT a.year) AS active_years,

    COUNT(DISTINCT a.article_id) AS total_articles,
    COUNT(DISTINCT ja.article_id) AS total_journal_articles,
    COUNT(DISTINCT ca.article_id) AS total_conference_articles,

    COUNT(DISTINCT ja.journal_id) AS distinct_journals,
    COUNT(DISTINCT ca.conference_id) AS distinct_conferences,

    COUNT(DISTINCT a.article_id) / NULLIF(COUNT(DISTINCT a.year), 0) AS avg_articles_per_year
FROM authors au
         JOIN article_authors aa
              ON au.author_id = aa.author_id
         JOIN articles a
              ON aa.article_id = a.article_id
         LEFT JOIN journal_articles ja
                   ON a.article_id = ja.article_id
         LEFT JOIN conference_articles ca
                   ON a.article_id = ca.article_id
WHERE au.author_id = ?
  AND a.year BETWEEN ? AND ?
GROUP BY
    au.author_id,
    au.author_name;