-- A7, A8, A9, A17: Journal profile statistics
-- Parameters:
--   1: journal_id
--   2: start_year
--   3: end_year

SELECT
    j.journal_id,
    j.journal_name,
    j.publisher_id,
    p.publisher_name,

    MIN(a.year) AS first_year,
    MAX(a.year) AS last_year,
    COUNT(DISTINCT a.year) AS active_years,

    COUNT(DISTINCT a.article_id) AS total_articles,
    COUNT(aa.author_id) AS total_author_occurrences,
    COUNT(DISTINCT aa.author_id) AS distinct_authors_all_time,

    COUNT(DISTINCT a.article_id) / NULLIF(COUNT(DISTINCT a.year), 0) AS avg_articles_per_year,
    COUNT(aa.author_id) / NULLIF(COUNT(DISTINCT a.year), 0) AS avg_author_occurrences_per_year,
    COUNT(aa.author_id) / NULLIF(COUNT(DISTINCT a.article_id), 0) AS avg_authors_per_article
FROM journals j
         LEFT JOIN publishers p
                   ON j.publisher_id = p.publisher_id
         JOIN journal_articles ja
              ON j.journal_id = ja.journal_id
         JOIN articles a
              ON ja.article_id = a.article_id
         LEFT JOIN article_authors aa
                   ON a.article_id = aa.article_id
WHERE j.journal_id = ?
  AND a.year BETWEEN ? AND ?
GROUP BY
    j.journal_id,
    j.journal_name,
    j.publisher_id,
    p.publisher_name;